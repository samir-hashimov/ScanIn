package com.scanin.dao.entity;

import com.scanin.util.ArrivalStatus;
import com.scanin.util.DepartureStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "attendances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "work_date"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "arrival_status", nullable = false, length = 30)
    private ArrivalStatus arrivalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "departure_status", nullable = false, length = 30)
    private DepartureStatus departureStatus;

    @Column(name = "is_first_to_arrive", nullable = false)
    private boolean isFirstToArrive;

    @Column(name = "is_last_to_leave", nullable = false)
    private boolean isLastToLeave;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;

    @Column(name = "overtime_minutes", nullable = false)
    private int overtimeMinutes;

    @Column(name = "total_worked_minutes", nullable = false)
    private int totalWorkedMinutes;

    @Column(name = "late_reason", length = 500)
    private String lateReason;

    @Column(name = "early_leave_reason", length = 500)
    private String earlyLeaveReason;

    @Column(name = "system_note", length = 255)
    private String systemNote;
}