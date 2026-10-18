package com.scanin.service;

import com.scanin.dao.entity.Attendance;
import com.scanin.dao.entity.User;
import com.scanin.dao.repository.AttendanceRepository;
import com.scanin.dto.request.ScanActionRequest;
import com.scanin.dto.response.AttendanceReportResponse;
import com.scanin.dto.response.ScanResponse;
import com.scanin.mapper.AttendanceMapper;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserService userService;
    private final OfficeIpService officeIpService;
    private final NotificationService notificationService;
    private final AttendanceMapper attendanceMapper;

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
                message = "🥇 Günün çempionu! Ofisin işıqlarını bu gün sən yandırdın, " + user.getFullName() + "! Kofe aparatı səni gözləyir.";
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

        // Hal 1: İşçi artıq bu gün çıxış edibsə və 3-cü dəfə oxudursa:
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

        // Hal 2: İşçi 30+ dəq gecikib, bayaq giriş edib, indi isə ekrandakı xanaya Gecikmə Səbəbini yazıb göndərirsə:
        if (attendance.getArrivalStatus() == ArrivalStatus.CRITICAL_LATE
                && attendance.getLateReason() == null
                && actionRequest != null
                && actionRequest.getReason() != null
                && !actionRequest.isForceEarlyCheckout()) {

            // YENİ VALİDASİYA: Boşluq və ya "a" yazıb keçə bilməsin!
            validateReasonText(actionRequest.getReason(), "Gecikmə səbəbi düzgün qeyd olunmayıb!");

            attendance.setLateReason(actionRequest.getReason().trim());
            attendanceRepository.save(attendance);
            return ScanResponse.builder()
                    .message("📝 Gecikmə səbəbin qeydə alındı: \"" + actionRequest.getReason() + "\". Xoş işlər, " + user.getFullName() + "!")
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .actionType("LATE_REASON_SAVED")
                    .scanTime(now)
                    .arrivalStatus(attendance.getArrivalStatus())
                    .departureStatus(attendance.getDepartureStatus())
                    .build();
        }

        // Hal 3: Səhər gəlib və İLK 10 DƏQİQƏ İÇİNDƏ təkrar QR oxudub (Səhvən əli dəyib və ya təcili işi çıxıb)
        long minutesSinceCheckIn = Duration.between(attendance.getCheckInTime(), now).toMinutes();

        if (minutesSinceCheckIn < 10 && (actionRequest == null || !actionRequest.isForceEarlyCheckout())) {
            long mins = Math.max(1, minutesSinceCheckIn);
            return ScanResponse.builder()
                    .message("👀 Dayan görüm, " + user.getFullName() + "! Cəmi " + mins + " dəqiqə əvvəl gəlmisən, hara gedirsən? Kreslon hələ isinməyib! Əgər doğrudan təcili işin çıxıbsa və gedirsənsə, səbəbini yaz:")
                    .employeeInfo(user.getFullName() + " (" + user.getPosition() + ")")
                    .actionType("COOLDOWN_WARNING")
                    .requiresEarlyLeaveConfirmation(true) // Ekranda seçim və səbəb xanasını açacaq!
                    .scanTime(now)
                    .arrivalStatus(attendance.getArrivalStatus())
                    .departureStatus(attendance.getDepartureStatus())
                    .build();
        }

        // Hal 4: Həqiqətən ÇIXIŞ (CHECK-OUT) edir:
        attendance.setCheckOutTime(now);

        int totalWorked = (int) Duration.between(attendance.getCheckInTime(), now).toMinutes();
        attendance.setTotalWorkedMinutes(totalWorked);

        // Əlavə işlədiyi dəqiqələrin (Overtime) hesablanması:
        LocalTime shiftEnd = user.getShiftEndTime(); // Məs: 18:00
        if (now.toLocalTime().isAfter(shiftEnd)) {
            int overtime = (int) Duration.between(shiftEnd, now.toLocalTime()).toMinutes();
            attendance.setOvertimeMinutes(overtime);
        }

        // Hal 4-ün içində: Əgər 10 dəq içində təcili səbəb yazıb tez çıxırsa:
        if (actionRequest != null && actionRequest.isForceEarlyCheckout()) {
            // YENİ VALİDASİYA: Təcili çıxırsa səbəb mütləq və ən azı 5 hərf olmalıdır!
            validateReasonText(actionRequest.getReason(), "Erkən çıxış üçün səbəb yazmaq məcburidir!");

            attendance.setDepartureStatus(DepartureStatus.EARLY_LEAVE);
            attendance.setEarlyLeaveReason(actionRequest.getReason().trim());
        } else {
            attendance.setDepartureStatus(DepartureStatus.COMPLETED);
        }

        // Əvvəlcə yadda saxlayırıq ki, bu işçinin statusu WORKING-dən çıxsın:
        attendanceRepository.save(attendance);

        // İndi yoxlayırıq: Ofisdə WORKING statusunda başqa kimsə qaldımı?
        long remainingWorkersInOffice = attendanceRepository.countByWorkDateAndDepartureStatus(today, DepartureStatus.WORKING);

        String checkoutMessage;
        if (remainingWorkersInOffice == 0) {
            // Ofisdə heç kim qalmayıb -> ƏN SON ÇIXAN BUDUR!
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
}