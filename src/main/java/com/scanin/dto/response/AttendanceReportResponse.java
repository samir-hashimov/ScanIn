package com.scanin.dto.response;

import com.scanin.util.ArrivalStatus;
import com.scanin.util.DepartureStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceReportResponse {
    private Long attendanceId;
    private String employeeCode;      // Məs: EMP-001 (3 dənə Nurayı ayırmaq üçün)
    private String fullName;          // Məs: Nuray Əliyeva
    private String position;          // Məs: Mühasib
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private ArrivalStatus arrivalStatus;
    private DepartureStatus departureStatus;
    private boolean isFirstToArrive;  // 🥇 İlk gələn
    private boolean isLastToLeave;    // 🌙 Son çıxan
    private int lateMinutes;          // Məs: 20 dəq
    private int overtimeMinutes;      // Məs: 20 dəq
    private int totalWorkedMinutes;   // Məs: 540 dəq (9 saat)
    private String lateReason;
    private String earlyLeaveReason;
    private String systemNote;        // Məs: ⚠️ Avtomatik bağlandı (Unudulub)
}