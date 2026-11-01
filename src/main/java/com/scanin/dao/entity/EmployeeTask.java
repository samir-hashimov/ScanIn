package com.scanin.dao.entity;

import com.scanin.util.TaskKind;
import com.scanin.util.TaskStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Gələcəkdə 1C-dən gələndə eyni tapşırığı təkrar yazmamaq üçün 1C-nin unikal ID-si (İndi boş da qala bilər)
    @Column(name = "external_1c_id", length = 100)
    private String external1cId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime pausedAt;

    private LocalDateTime lastResumedAt;

    @Builder.Default
    private Integer spentMinutes = 0;

    /** Planlaşdırılan vaxt (əsasən əlavə fəaliyyətlər üçün, dəqiqə) */
    private Integer allocatedMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TaskKind kind = TaskKind.ASSIGNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paused_from_task_id")
    private EmployeeTask pausedFromTask;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}