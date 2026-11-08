package com.scanin.controller;

import com.scanin.dao.entity.Attendance;
import com.scanin.dao.entity.EmployeeTask;
import com.scanin.dao.entity.User;
import com.scanin.dao.repository.AttendanceRepository;
import com.scanin.dao.repository.EmployeeTaskRepository;
import com.scanin.dao.repository.UserRepository;
import com.scanin.exception.ResourceNotFoundException;
import com.scanin.service.TaskService;
import com.scanin.util.Role;
import com.scanin.util.TaskKind;
import com.scanin.util.TaskStatus;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/1c")
@RequiredArgsConstructor
public class OneCSyncController {
    private volatile boolean employeeSyncRequested = true;
    private final UserRepository userRepository;
    private final EmployeeTaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;

    // 1. 1C-də İşçi (Employees) yaradılanda və ya dəyişdiriləndə Java bazasına sinxronizasiya edir:
    @PostMapping("/employee")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncEmployeeFrom1C(@RequestBody OneCEmployeeDto dto) {
        String empCode = normalizeCodeToJava(dto.getCode()); // "000000001" -> "EMP-001"
        String fullName = ((dto.getFirstName() != null ? dto.getFirstName() : "") + " " +
                (dto.getLastName() != null ? dto.getLastName() : "")).trim();

        User user = userRepository.findByEmployeeCode(empCode).orElseGet(() -> User.builder()
                .employeeCode(empCode)
                .role(Role.EMPLOYEE)
                .shiftStartTime(LocalTime.of(9, 0))
                .shiftEndTime(LocalTime.of(18, 0))
                .build());

        user.setFullName(fullName.isEmpty() ? dto.getDescription() : fullName);
        user.setPosition(dto.getPosition() != null && !dto.getPosition().isBlank() ? dto.getPosition() : "İşçi");
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "employeeCode", empCode,
                "message", "İşçi ScanIn bazasına uğurla sinxronizasiya olundu!"
        ));
    }

    // 2. 1C-də "WorkTasks" sənədi yazılıb təsdiqlənəndə birbaşa Java-dakı işçinin panelinə düşür:
    @PostMapping("/task")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncTaskFrom1C(@RequestBody OneCTaskDto dto) {
        String empCode = normalizeCodeToJava(dto.getEmployeeCode()); // "000000001" -> "EMP-001"

        User user = userRepository.findByEmployeeCode(empCode)
                .orElseThrow(() -> new ResourceNotFoundException("ScanIn bazasında bu kodla işçi tapılmadı: " + empCode));

        EmployeeTask task = taskRepository.findByExternal1cId(dto.getExternal1cId())
                .orElseGet(() -> EmployeeTask.builder()
                        .external1cId(dto.getExternal1cId())
                        .status(TaskStatus.PENDING)
                        .kind(TaskKind.ASSIGNED)
                        .spentMinutes(0)
                        .createdAt(LocalDateTime.now())
                        .build());

        task.setUser(user);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setDeadline(dto.getDeadline() != null ? dto.getDeadline() : LocalDateTime.now().plusHours(4));
        task.setAssignedBy(dto.getAssignedBy() != null && !dto.getAssignedBy().isBlank()
                ? dto.getAssignedBy().trim()
                : "HR / Müdiriyyət");
        task.setPriority(normalizePriority(dto.getPriority()));

        taskRepository.save(task);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "taskId", task.getId(),
                "employeeCode", empCode
        ));
    }

    // 3. 1C sorğu atanda Java-dakı Giriş-Çıxışları (Attendance) və Tapşırıq Statuslarını 1C-yə qaytarır:
    @GetMapping("/pull")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> pullDataFor1C(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<Attendance> attendances = attendanceRepository.findAllByWorkDateOrderByCheckInTimeAsc(targetDate);

        List<Map<String, Object>> attendanceList = new ArrayList<>();
        for (Attendance a : attendances) {
            Map<String, Object> map = new HashMap<>();
            map.put("attendanceId", a.getId());
            map.put("employeeCodeJava", a.getUser().getEmployeeCode()); // məs: "EMP-001"
            map.put("employeeCode1C", convertCodeTo1C(a.getUser().getEmployeeCode())); // məs: "000000001"
            map.put("fullName", a.getUser().getFullName());
            map.put("position", a.getUser().getPosition() != null ? a.getUser().getPosition() : "Developer");
            map.put("workDate", a.getWorkDate().toString() + "T00:00:00");
            map.put("checkInTime", a.getCheckInTime() != null ? a.getCheckInTime().withNano(0).toString() : "");
            map.put("checkOutTime", a.getCheckOutTime() != null ? a.getCheckOutTime().withNano(0).toString() : "");

            String comment = "";
            if (a.isFieldVisit()) {
                comment += "[Kontragent: " + a.getCounterpartyName() + " | HR: " + a.getApprovalStatus() + "] ";
            }
            if (a.getLateReason() != null && !a.getLateReason().isBlank()) {
                comment += "Gecikmə: " + a.getLateReason() + " ";
            }
            if (a.getEarlyLeaveReason() != null && !a.getEarlyLeaveReason().isBlank()) {
                comment += "Tez çıxış: " + a.getEarlyLeaveReason();
            }
            map.put("comment", comment.trim());
            attendanceList.add(map);
        }

        List<EmployeeTask> allTasks = taskRepository.findAllByOrderByDeadlineAsc();
        List<Map<String, Object>> taskList = new ArrayList<>();
        for (EmployeeTask t : allTasks) {
            if (t.getExternal1cId() != null && !t.getExternal1cId().isBlank()) {
                Map<String, Object> tm = new HashMap<>();
                tm.put("external1cId", t.getExternal1cId());
                tm.put("status", t.getStatus().name()); // PENDING, IN_PROGRESS, COMPLETED
                int spent = liveSpentMinutes(t);
                String completionComment = t.getCompletionComment() != null ? t.getCompletionComment() : "";
                String commentSummary = "Sərf olunan vaxt: " + spent + " dəq.";
                if (!completionComment.isBlank()) {
                    commentSummary += " | Qeyd: " + completionComment;
                }
                tm.put("spentMinutes", spent);
                tm.put("startedAt", t.getStartedAt() != null ? t.getStartedAt().withNano(0).toString() : "");
                tm.put("completedAt", t.getCompletedAt() != null ? t.getCompletedAt().withNano(0).toString() : "");
                tm.put("assignedBy", t.getAssignedBy() != null ? t.getAssignedBy() : "HR / Müdiriyyət");
                tm.put("priority", normalizePriority(t.getPriority()));
                tm.put("completionComment", completionComment);
                tm.put("commentSummary", commentSummary);
                taskList.add(tm);
            }
        }

        return ResponseEntity.ok(Map.of(
                "attendances", attendanceList,
                "tasks", taskList,
                "syncEmployeesNeeded", this.employeeSyncRequested
        ));
    }

    @PostMapping("/request-employees-sync")
    public Map<String, Object> requestEmployeesSync() {
        this.employeeSyncRequested = true;
        return Map.of(
                "success", true,
                "message", "Sinxronizasiya sorğusu göndərildi!"
        );
    }

    @PostMapping("/employees/batch")
    @Transactional
    public Map<String, Object> syncAllEmployeesBatch(@RequestBody List<OneCEmployeeDto> employees) {
        int count = 0;
        for (OneCEmployeeDto dto : employees) {
            String empCode = normalizeCodeToJava(dto.getCode());
            if (empCode.isEmpty()) continue;

            String fullName = ((dto.getFirstName() != null ? dto.getFirstName() : "") + " " +
                    (dto.getLastName() != null ? dto.getLastName() : "")).trim();
            String finalName = fullName.isEmpty() ? dto.getDescription() : fullName;

            User user = userRepository.findByEmployeeCode(empCode).orElseGet(() -> User.builder()
                    .employeeCode(empCode)
                    .role(Role.EMPLOYEE)
                    .shiftStartTime(LocalTime.of(9, 0))
                    .shiftEndTime(LocalTime.of(18, 0))
                    .build());

            user.setFullName(finalName != null ? finalName : empCode);
            user.setPosition(dto.getPosition() != null && !dto.getPosition().isBlank() ? dto.getPosition() : "Developer");
            userRepository.save(user);
            count++;
        }
        this.employeeSyncRequested = false;

        return Map.of(
                "success", true,
                "syncedCount", count,
                "message", count + " işçi 1C-dən köçürüldü!"
        );
    }


    /**
     * 1C-dən gələn prioritet sözlərini ScanIn standartına çevirir:
     * Yüksək / Yüksən / HIGH / Высокий -> HIGH
     * Aşağı / LOW / Низкий -> LOW
     * qalan hallar -> NORMAL
     */
    public static String normalizePriority(String raw) {
        if (raw == null || raw.isBlank()) {
            return "NORMAL";
        }
        String original = raw.trim();
        String v = original.toLowerCase(Locale.ROOT)
                .replace("ı", "i")
                .replace("ə", "e")
                .replace("ö", "o")
                .replace("ü", "u")
                .replace("ş", "s")
                .replace("ç", "c")
                .replace("ğ", "g");

        if (v.equals("yuksek") || v.equals("yuksen") || v.equals("high")
                || original.equalsIgnoreCase("Высокий") || v.equals("высокий")) {
            return "HIGH";
        }
        if (v.equals("asagi") || v.equals("low")
                || original.equalsIgnoreCase("Низкий") || v.equals("низкий")) {
            return "LOW";
        }
        return "NORMAL";
    }

    private int liveSpentMinutes(EmployeeTask t) {
        int spent = t.getSpentMinutes() != null ? t.getSpentMinutes() : 0;
        if (t.getStatus() == TaskStatus.IN_PROGRESS && t.getLastResumedAt() != null) {
            long add = Duration.between(t.getLastResumedAt(), LocalDateTime.now()).toMinutes();
            spent += (int) Math.max(0, add);
        }
        return spent;
    }

    // "000000001" -> "EMP-001" çeviricisi:
    private String normalizeCodeToJava(String rawCode) {
        if (rawCode == null) return "";
        String clean = rawCode.trim().toUpperCase();
        if (clean.startsWith("EMP-")) return clean;
        try {
            int num = Integer.parseInt(clean);
            return String.format("EMP-%03d", num);
        } catch (NumberFormatException e) {
            return clean;
        }
    }

    // "EMP-001" -> "000000001" çeviricisi:
    private String convertCodeTo1C(String javaCode) {
        if (javaCode == null) return "";
        String digits = javaCode.replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            return String.format("%09d", Integer.parseInt(digits));
        }
        return javaCode;
    }

    @Data
    public static class OneCEmployeeDto {
        private String code;
        private String description;
        private String firstName;
        private String lastName;
        private String position;
        private String phone;
    }

    @Data
    public static class OneCTaskDto {
        private String external1cId;
        private String employeeCode;
        private String title;
        private String description;
        private LocalDateTime deadline;
        private String assignedBy;
        private String priority;
    }
}