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
    private String employeeCode;
    private String fullName;
    private String position;
    private LocalDate workDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private ArrivalStatus arrivalStatus;
    private DepartureStatus departureStatus;
    private boolean isFirstToArrive;
    private boolean isLastToLeave;
    private int lateMinutes;
    private int overtimeMinutes;
    private int totalWorkedMinutes;
    private String lateReason;
    private String earlyLeaveReason;
    private String systemNote;
    private boolean fieldVisit;
    private String counterpartyName;
    private String approvalStatus;
    private Double latitude;
    private Double longitude;
    private boolean external;
    private String locationNote;
}