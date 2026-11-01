package com.scanin.dto.response;

import com.scanin.util.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String employeeCode;
    private String employeeFullName;
    private String employeePosition;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private TaskStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer spentMinutes;
    private boolean overdue; // Deadline keçibsə true olacaq (ekranda qırmızı yanması üçün)
}