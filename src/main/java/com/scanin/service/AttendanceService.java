package com.scanin.service;

import com.scanin.dao.entity.Attendance;
import com.scanin.dao.entity.User;
import com.scanin.dao.repository.AttendanceRepository;
import com.scanin.dao.repository.UserRepository;
import com.scanin.dto.request.FieldVisitRequest;
import com.scanin.dto.request.ScanActionRequest;
import com.scanin.dto.response.AttendanceReportResponse;
import com.scanin.dto.response.ScanResponse;
import com.scanin.exception.ResourceNotFoundException;
import com.scanin.mapper.AttendanceMapper;
import com.scanin.util.ApprovalStatus;
import com.scanin.util.ArrivalStatus;
import com.scanin.util.DepartureStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserService userService;
    private final OfficeIpService officeIpService;
    private final NotificationService notificationService;
    private final AttendanceMapper attendanceMapper;
    private final UserRepository userRepository;

    // İşçi QR oxudanda çağırılan ƏSAS METOD
    @Transactional
    public ScanResponse processQrScan(String username, ScanActionRequest actionRequest, HttpServletRequest httpRequest) {
        // 1. Əvvəlcə işçinin ofis Wi-Fi-da olub-olmadığını yoxlayırıq!
        officeIpService.validateClientIp(httpRequest);

        // 2. İşçini bazadan tapırıq
        User user = userService.getByUsername(username);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 3. Baxırıq: Bu işçi bu gün artıq giriş edibmi?
        Optional<Attendance> existingRecord = attendanceRepository.findByUserAndWorkDate(user, today);

        if (existingRecord.isEmpty()) {
            // Səhər ilk dəfə oxudur -> GİRİŞ (CHECK-IN)
            return handleCheckIn(user, today, now);
        } else {
            // Artıq bu gün girişi var -> Ya gecikmə səbəbini göndərir, ya da ÇIXIŞ (CHECK-OUT) edir
            return handleExistingRecord(existingRecord.get(), actionRequest, today, now);
        }
    }

    // ==================== GİRİŞ (CHECK-IN) MƏNTİQİ ====================
    private ScanResponse handleCheckIn(User user, LocalDate today, LocalDateTime now) {
        // Bu gün ofisə gələn İLK adam budurmu? (Bazada bu gün üçün heç bir qeyd yoxdursa -> true)
        boolean isFirstOfDay = !attendanceRepository.existsByWorkDate(today);

        LocalTime shiftStart = user.getShiftStartTime(); // Məs: 09:00
        LocalTime arrivalTime = now.toLocalTime();

        int lateMinutes = 0;
        ArrivalStatus arrivalStatus = ArrivalStatus.ON_TIME;
        boolean requiresLateReason = false;
        String message;

        // Gecikmə hesablama:
        if (arrivalTime.isAfter(shiftStart)) {
            lateMinutes = (int) Duration.between(shiftStart, arrivalTime).toMinutes();
        }

        if (lateMinutes == 0) {
            arrivalStatus = ArrivalStatus.ON_TIME;
            if (isFirstOfDay) {
                message = "🥇 Günün çempionu! Ofisin işıqlarını bu gün sən yandırdın, " + user.getFullName();
            } else {
                message = "✅ Xoş gəldin, " + user.getFullName() + "! Tam vaxtında iş başındasan, uğurlu günlər!";
            }
        } else if (lateMinutes < 30) {
            // 1 - 29 dəqiqə gecikmə: Qeyd tələb etmir, amma bazaya LATE düşür
            arrivalStatus = ArrivalStatus.LATE;
            if (isFirstOfDay) {
                message = "🥇 Günün ilk gələnisən, amma " + lateMinutes + " dəqiqə gecikmisən! 😅 Yarım saatı keçmədiyin üçün bu dəfə sorğu-sualsız keçirik.";
            } else {
                message = "😅 Nəfəsini dər, " + user.getFullName() + "! " + lateMinutes + " dəqiqə gecikdin, amma 30 dəqiqə limitini keçmədiyin üçün izahat yazmaqdan qurtuldun!";
            }
        } else {
            // 30+ dəqiqə gecikmə: Fərqli status (CRITICAL_LATE) və Məcburi Səbəb!
            arrivalStatus = ArrivalStatus.CRITICAL_LATE;
            requiresLateReason = true;
            message = "☕ Deyəsən Bakı tıxacı səni də əsir götürüb (yoxsa şirin yuxu?). " + lateMinutes + " dəqiqə gecikmisən! HR-la aramız dəyməsin deyə zəhmət olmasa gecikmə səbəbini qeyd et:";
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .workDate(today)
                .checkInTime(now)
                .arrivalStatus(arrivalStatus)
                .departureStatus(DepartureStatus.WORKING)
                .isFirstToArrive(isFirstOfDay)
                .isLastToLeave(false)
                .lateMinutes(lateMinutes)
                .overtimeMinutes(0)
                .totalWorkedMinutes(0)
                .build();

        attendanceRepository.save(attendance);
        notificationService.sendCheckInNotification(attendance);

        return ScanResponse.builder()
                .message(message)
                .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                .actionType("CHECK_IN")
                .requiresLateReason(requiresLateReason)
                .requiresEarlyLeaveConfirmation(false)
                .scanTime(now)
                .arrivalStatus(arrivalStatus)
                .departureStatus(DepartureStatus.WORKING)
                .build();
    }

    // ==================== ÇIXIŞ VƏ ƏLAVƏ ƏMƏLİYYATLAR ====================
    private ScanResponse handleExistingRecord(Attendance attendance, ScanActionRequest actionRequest, LocalDate today, LocalDateTime now) {
        User user = attendance.getUser();

        // Hal 1: İşçi artıq bu gün çıxış edibsə:
        if (attendance.getDepartureStatus() != DepartureStatus.WORKING) {
            return ScanResponse.builder()
                    .message("🏠 " + user.getFullName() + ", sən bu gün üçün artıq çıxış etmisən (Saat: " + attendance.getCheckOutTime().toLocalTime() + "). Sabah görüşənədək!")
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .actionType("ALREADY_CHECKED_OUT")
                    .scanTime(now)
                    .arrivalStatus(attendance.getArrivalStatus())
                    .departureStatus(attendance.getDepartureStatus())
                    .build();
        }

        // Hal 2: Əgər işçinin statusu CRITICAL_LATE-dirsə və hələ də gecikmə səbəbi YOXDURSA (refresh etsə belə bura düşəcək):
        if (attendance.getArrivalStatus() == ArrivalStatus.CRITICAL_LATE && (attendance.getLateReason() == null || attendance.getLateReason().isBlank())) {
            if (actionRequest == null || actionRequest.getReason() == null) {
                return ScanResponse.builder()
                        .message("⏰ " + user.getFullName() + ", gecikmə səbəbini hələ qeyd etməmisən! Zəhmət olmasa səbəbi yaz:")
                        .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                        .actionType("LATE_REASON_REQUIRED")
                        .requiresLateReason(true)
                        .scanTime(now)
                        .arrivalStatus(attendance.getArrivalStatus())
                        .departureStatus(attendance.getDepartureStatus())
                        .build();
            }

            validateReasonText(actionRequest.getReason(), "Gecikmə səbəbi düzgün qeyd olunmayıb!");

            attendance.setLateReason(actionRequest.getReason().trim());
            attendanceRepository.save(attendance);
            return ScanResponse.builder()
                    .message("📝 Gecikmə səbəbin qeydə alındı: \"" + attendance.getLateReason() + "\". Xoş işlər, " + user.getFullName() + "!")
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .actionType("LATE_REASON_SAVED")
                    .scanTime(now)
                    .arrivalStatus(attendance.getArrivalStatus())
                    .departureStatus(attendance.getDepartureStatus())
                    .build();
        }

        // İşin bitmə saatını və girişdən keçən dəqiqəni hesablayırıq:
        long minutesSinceCheckIn = Duration.between(attendance.getCheckInTime(), now).toMinutes();
        LocalTime shiftEnd = user.getShiftEndTime() != null ? user.getShiftEndTime() : LocalTime.of(18, 0);
        boolean isBeforeShiftEnd = now.toLocalTime().isBefore(shiftEnd);

        // Hal 3: İlk 10 dəqiqə içində oxudub VƏ YA iş vaxtı hələ bitməyib (Tez çıxış cəhdi):
        if ((minutesSinceCheckIn < 10 || isBeforeShiftEnd) && (actionRequest == null || !actionRequest.isForceEarlyCheckout())) {
            String warningMsg;
            if (minutesSinceCheckIn < 10) {
                long mins = Math.max(1, minutesSinceCheckIn);
                warningMsg = "👀 Dayan görüm, " + user.getFullName() + "! Cəmi " + mins + " dəqiqə əvvəl gəlmisən, hara gedirsən? Kreslon hələ isinməyib! Əgər doğrudan təcili işin çıxıbsa və gedirsənsə, səbəbini yaz:";
            } else {
                long minsLeft = Duration.between(now.toLocalTime(), shiftEnd).toMinutes();
                warningMsg = "⏳ " + user.getFullName() + ", iş vaxtının bitməsinə hələ " + minsLeft + " dəqiqə qalıb (Bitmə saatı: " + shiftEnd + "). Tez çıxma səbəbini qeyd et:";
            }

            return ScanResponse.builder()
                    .message(warningMsg)
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .actionType("COOLDOWN_WARNING")
                    .requiresEarlyLeaveConfirmation(true)
                    .scanTime(now)
                    .arrivalStatus(attendance.getArrivalStatus())
                    .departureStatus(attendance.getDepartureStatus())
                    .build();
        }

        // Hal 4: Həqiqətən ÇIXIŞ (CHECK-OUT) edir:
        if ((actionRequest != null && actionRequest.isForceEarlyCheckout()) || isBeforeShiftEnd) {
            String reason = (actionRequest != null) ? actionRequest.getReason() : null;
            validateReasonText(reason, "Erkən çıxış üçün səbəb yazmaq məcburidir!");

            attendance.setDepartureStatus(DepartureStatus.EARLY_LEAVE);
            attendance.setEarlyLeaveReason(reason.trim());
        } else {
            attendance.setDepartureStatus(DepartureStatus.COMPLETED);
        }

        attendance.setCheckOutTime(now);
        int totalWorked = (int) Duration.between(attendance.getCheckInTime(), now).toMinutes();
        attendance.setTotalWorkedMinutes(totalWorked);

        if (now.toLocalTime().isAfter(shiftEnd)) {
            int overtime = (int) Duration.between(shiftEnd, now.toLocalTime()).toMinutes();
            attendance.setOvertimeMinutes(overtime);
        }

        attendanceRepository.save(attendance);

        long remainingWorkersInOffice = attendanceRepository.countByWorkDateAndDepartureStatus(today, DepartureStatus.WORKING);

        String checkoutMessage;
        if (remainingWorkersInOffice == 0) {
            attendance.setLastToLeave(true);
            attendanceRepository.save(attendance);
            checkoutMessage = "🦸‍♂️ Ofisin gizli qəhrəmanı! Hamı getdi, son nöqtəni sən qoydun. Bizim üçün çəkdiyin bu qədər əziyyətə görə təşəkkürlər, " + user.getFullName() + "! İşıqları söndürməyi unutma, xoş istirahətlər! 🌙";
        } else {
            checkoutMessage = "👋 Yaxşı yol, " + user.getFullName() + "! Bu gün ümumi " + (totalWorked / 60) + " saat " + (totalWorked % 60) + " dəqiqə işlədin. Sabaha qədər!";
        }

        notificationService.sendCheckOutNotification(attendance);

        return ScanResponse.builder()
                .message(checkoutMessage)
                .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                .actionType("CHECK_OUT")
                .scanTime(now)
                .arrivalStatus(attendance.getArrivalStatus())
                .departureStatus(attendance.getDepartureStatus())
                .build();
    }

    // ==================== HR HESABAT METODU ====================
    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getDailyReport(LocalDate date) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        List<Attendance> records = attendanceRepository.findAllByWorkDateOrderByCheckInTimeAsc(targetDate);
        return attendanceMapper.toReportResponseList(records);
    }

    // Köməkçi Validasiya Metodu: Səbəb yazılıbsa, boşluqdan və ya 1-2 hərfdən ibarət ola bilməz!
    private void validateReasonText(String reason, String errorMessage) {
        if (reason == null || reason.trim().length() < 5) {
            throw new IllegalArgumentException(errorMessage + " (Ən azı 5 simvolluq real səbəb qeyd edin!)");
        }
    }

    // Kənardan / səfərdən giriş-çıxış: ofis IP-si və HR təsdiqi tələb olunmur, dərhal qeydə alınır.
    @Transactional
    public ScanResponse recordFieldVisit(String username, FieldVisitRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı!"));

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        FieldVisitRequest payload = request != null ? request : new FieldVisitRequest();

        Optional<Attendance> existingOpt = attendanceRepository.findByUserAndWorkDate(user, today);

        if (existingOpt.isEmpty()) {
            LocalTime shiftStart = user.getShiftStartTime() != null ? user.getShiftStartTime() : LocalTime.of(9, 0);
            int lateMinutes = 0;
            if (now.toLocalTime().isAfter(shiftStart)) {
                lateMinutes = (int) Duration.between(shiftStart, now.toLocalTime()).toMinutes();
            }

            ArrivalStatus arrivalStatus;
            if (lateMinutes == 0) {
                arrivalStatus = ArrivalStatus.ON_TIME;
            } else if (lateMinutes < 30) {
                arrivalStatus = ArrivalStatus.LATE;
            } else {
                arrivalStatus = ArrivalStatus.CRITICAL_LATE;
            }

            Attendance attendance = Attendance.builder()
                    .user(user)
                    .workDate(today)
                    .checkInTime(now)
                    .arrivalStatus(arrivalStatus)
                    .departureStatus(DepartureStatus.WORKING)
                    .isFirstToArrive(false)
                    .isLastToLeave(false)
                    .lateMinutes(lateMinutes)
                    .overtimeMinutes(0)
                    .totalWorkedMinutes(0)
                    .approvalStatus(ApprovalStatus.NOT_REQUIRED)
                    .build();
            applyExternalLocation(attendance, payload, false);
            attendanceRepository.save(attendance);
            notificationService.sendCheckInNotification(attendance);

            String place = placeLabel(attendance);
            String gpsHint = attendance.getLatitude() != null
                    ? " GPS koordinatları qeydə alındı."
                    : " Lokasiya icazəsi olmadığı üçün koordinatsız, kənardan giriş kimi saxlanıldı.";

            return ScanResponse.builder()
                    .actionType("FIELD_CHECK_IN")
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .message("📍 " + place + " GİRİŞ dərhal qeydə alındı." + gpsHint)
                    .scanTime(now)
                    .arrivalStatus(arrivalStatus)
                    .departureStatus(DepartureStatus.WORKING)
                    .build();
        }

        Attendance attendance = existingOpt.get();
        if (attendance.getCheckOutTime() != null || attendance.getDepartureStatus() != DepartureStatus.WORKING) {
            throw new IllegalStateException("Siz bu gün üçün artıq çıxış etmisiniz!");
        }

        LocalTime shiftEnd = user.getShiftEndTime() != null ? user.getShiftEndTime() : LocalTime.of(18, 0);
        boolean early = now.toLocalTime().isBefore(shiftEnd);
        attendance.setCheckOutTime(now);
        attendance.setDepartureStatus(early ? DepartureStatus.EARLY_LEAVE : DepartureStatus.COMPLETED);
        attendance.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);

        int workedMins = (int) Math.max(0, Duration.between(attendance.getCheckInTime(), now).toMinutes());
        attendance.setTotalWorkedMinutes(workedMins);
        if (now.toLocalTime().isAfter(shiftEnd)) {
            attendance.setOvertimeMinutes((int) Duration.between(shiftEnd, now.toLocalTime()).toMinutes());
        } else {
            attendance.setOvertimeMinutes(0);
        }

        applyExternalLocation(attendance, payload, true);
        attendanceRepository.save(attendance);
        notificationService.sendCheckOutNotification(attendance);

        String place = placeLabel(attendance);
        String gpsHint = payload.getLatitude() != null && payload.getLongitude() != null
                ? " Çıxış koordinatı da qeydə alındı."
                : "";

        return ScanResponse.builder()
                .actionType("FIELD_CHECK_OUT")
                .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                .message("✅ " + place + " ÇIXIŞ dərhal qeydə alındı. Bu gün " + (workedMins / 60) + " saat " + (workedMins % 60) + " dəqiqə işlədiniz." + gpsHint)
                .scanTime(now)
                .arrivalStatus(attendance.getArrivalStatus())
                .departureStatus(attendance.getDepartureStatus())
                .build();
    }

    private void applyExternalLocation(Attendance attendance, FieldVisitRequest request, boolean checkout) {
        attendance.setFieldVisit(true);
        attendance.setExternal(true);
        attendance.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);

        if (request.getCounterpartyName() != null && !request.getCounterpartyName().isBlank()) {
            attendance.setCounterpartyName(request.getCounterpartyName().trim());
        }

        String note = firstNonBlank(request.getLocationNote(), request.getNote());
        if (note != null) {
            attendance.setLocationNote(mergeNote(attendance.getLocationNote(), note));
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            return;
        }

        if (!checkout || attendance.getLatitude() == null || attendance.getLongitude() == null) {
            attendance.setLatitude(request.getLatitude());
            attendance.setLongitude(request.getLongitude());
            return;
        }

        String checkoutGps = String.format(Locale.US, "Çıxış GPS: %.5f, %.5f", request.getLatitude(), request.getLongitude());
        attendance.setLocationNote(mergeNote(attendance.getLocationNote(), checkoutGps));
    }

    private String placeLabel(Attendance attendance) {
        if (attendance.getCounterpartyName() != null && !attendance.getCounterpartyName().isBlank()) {
            return attendance.getCounterpartyName() + " yanında";
        }
        return "Kənardan";
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private String mergeNote(String existing, String addition) {
        if (addition == null || addition.isBlank()) {
            return existing;
        }
        String trimmed = addition.trim();
        if (existing == null || existing.isBlank()) {
            return limitNote(trimmed);
        }
        if (existing.contains(trimmed)) {
            return existing;
        }
        return limitNote(existing + " | " + trimmed);
    }

    private String limitNote(String value) {
        if (value == null || value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }

    // Köhnə HR təsdiq endpoint-i artıq rədd etmir: kənardan giriş birbaşa etibarlı sayılır.
    @Transactional
    public void verifyFieldVisit(Long attendanceId, boolean approved) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Qeyd tapılmadı!"));

        attendance.setFieldVisit(true);
        attendance.setExternal(true);
        attendance.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
        attendance.setSystemNote("Kənardan giriş birbaşa qeydə alınıb, HR təsdiqi tələb olunmur.");
        attendanceRepository.save(attendance);
    }
}