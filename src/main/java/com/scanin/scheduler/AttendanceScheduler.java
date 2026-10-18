package com.scanin.scheduler;

import com.scanin.dao.entity.Attendance;
import com.scanin.dao.repository.AttendanceRepository;
import com.scanin.util.DepartureStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceScheduler {

    private final AttendanceRepository attendanceRepository;

    /**
     * Hər gecə saat 23:59:00-da (Bakı vaxtı ilə) avtomatik işə düşür.
     * Cron formatı: "Saniyə Dəqiqə Saat Gün Ay Həftənin_günü"
     */
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Baku")
    @Transactional
    public void autoCloseForgottenCheckouts() {
        LocalDate today = LocalDate.now();
        log.info("🕚 [SCHEDULER] {} tarixi üçün çıxışı unudan işçilər yoxlanılır...", today);

        // 1. Bu gün işə gəlmiş, amma hələ də statusu WORKING (çıxış etməmiş) qalanları tapırıq:
        List<Attendance> forgottenRecords = attendanceRepository
                .findAllByWorkDateAndDepartureStatus(today, DepartureStatus.WORKING);

        if (forgottenRecords.isEmpty()) {
            log.info("✅ [SCHEDULER] Bu gün bütün işçilər vaxtında çıxış edib. Unudulan qeyd yoxdur.");
            return;
        }

        for (Attendance record : forgottenRecords) {
            // 2. İşçinin rəsmi iş bitmə saatını götürürük (məs: 18:00)
            LocalTime officialEndTime = record.getUser().getShiftEndTime();
            LocalDateTime defaultCheckOut = LocalDateTime.of(today, officialEndTime);

            // Nadir hal: Əgər işçi axşam 18:00-dan SONRA işə gəlibsə və çıxışı unudubsa,
            // mənfi (-) iş saatı yaranmasın deyə çıxış saatını elə giriş saatına bərabər edirik:
            if (record.getCheckInTime().isAfter(defaultCheckOut)) {
                defaultCheckOut = record.getCheckInTime();
            }

            // 3. Standart çıxış saatını (18:00) yazırıq ki, iş saatı 0 görünməsin:
            record.setCheckOutTime(defaultCheckOut);

            // 4. Ümumi işlədiyi dəqiqəni rəsmi çıxış saatına (18:00-a) görə hesablayırıq:
            int workedMinutes = (int) Duration.between(record.getCheckInTime(), defaultCheckOut).toMinutes();
            record.setTotalWorkedMinutes(Math.max(0, workedMinutes));

            // 5. Cərimə/Qoruma: İşçi qəsdən vurmamazlıq etməsin deyə əlavə iş saatını (Overtime) 0-layırıq!
            record.setOvertimeMinutes(0);

            // 6. Statusu FORGOT_CHECKOUT edirik və HR üçün xüsusi xəbərdarlıq qeydi qoyuruq:
            record.setDepartureStatus(DepartureStatus.FORGOT_CHECKOUT);
            record.setSystemNote("⚠️ Avtomatik bağlandı (Çıxışda QR oxudulmayıb)");

            log.warn("⚠️ [FORGOT_CHECKOUT] {} ({}) çıxışı unutduğu üçün saat {}-a avtomatik bağlandı.",
                    record.getUser().getFullName(),
                    record.getUser().getEmployeeCode(),
                    officialEndTime);
        }

        // 7. Hamısını bir dəfəyə bazada yeniləyirik:
        attendanceRepository.saveAll(forgottenRecords);
        log.info("✅ [SCHEDULER] Cəmi {} işçinin açıq qalmış iş günü avtomatik bağlandı.", forgottenRecords.size());
    }
}