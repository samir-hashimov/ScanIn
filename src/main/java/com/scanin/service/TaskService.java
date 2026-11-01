package com.scanin.service;

import com.scanin.dao.entity.EmployeeTask;
import com.scanin.dao.entity.User;
import com.scanin.dao.repository.EmployeeTaskRepository;
import com.scanin.dao.repository.UserRepository;
import com.scanin.dto.request.CreateTaskRequest;
import com.scanin.dto.response.TaskResponse;
import com.scanin.exception.ResourceNotFoundException;
import com.scanin.util.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final EmployeeTaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createOrSyncTask(CreateTaskRequest request) {
        User user = userRepository.findByEmployeeCode(request.getEmployeeCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Bu kodla işçi tapılmadı: " + request.getEmployeeCode()));

        if (request.getExternal1cId() != null && !request.getExternal1cId().isBlank()) {
            var existing = taskRepository.findByExternal1cId(request.getExternal1cId());
            if (existing.isPresent()) {
                EmployeeTask task = existing.get();
                task.setTitle(request.getTitle());
                task.setDescription(request.getDescription());
                task.setDeadline(request.getDeadline());
                return toResponse(taskRepository.save(task));
            }
        }

        EmployeeTask task = EmployeeTask.builder()
                .user(user)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .deadline(request.getDeadline())
                .external1cId(request.getExternal1cId())
                .status(TaskStatus.PENDING)
                .spentMinutes(0)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı!"));

        return taskRepository.findAllByUserOrderByDeadlineAsc(user)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskResponse startTask(Long taskId, String username) {
        EmployeeTask task = getOwnedTask(taskId, username);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Bu tapşırıq artıq tamamlanıb!");
        }

        if (task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        task.setStatus(TaskStatus.IN_PROGRESS);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse completeTask(Long taskId, String username) {
        EmployeeTask task = getOwnedTask(taskId, username);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Bu tapşırıq artıq tamamlanıb!");
        }

        LocalDateTime now = LocalDateTime.now();
        if (task.getStartedAt() == null) {
            task.setStartedAt(now);
        }
        task.setCompletedAt(now);
        task.setStatus(TaskStatus.COMPLETED);

        long mins = Duration.between(task.getStartedAt(), now).toMinutes();
        task.setSpentMinutes((int) Math.max(1, mins)); // Ən azı 1 dəqiqə qeyd olunsun

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasksForHr() {
        return taskRepository.findAllByOrderByDeadlineAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private EmployeeTask getOwnedTask(Long taskId, String username) {
        EmployeeTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tapşırıq tapılmadı!"));

        if (!task.getUser().getUsername().equals(username)) {
            throw new IllegalStateException("Bu tapşırıq sizə aid deyil!");
        }
        return task;
    }

    private TaskResponse toResponse(EmployeeTask t) {
        boolean isOverdue = t.getStatus() != TaskStatus.COMPLETED
                && t.getDeadline() != null
                && LocalDateTime.now().isAfter(t.getDeadline());

        return TaskResponse.builder()
                .id(t.getId())
                .employeeCode(t.getUser().getEmployeeCode())
                .employeeFullName(t.getUser().getFullName())
                .employeePosition(t.getUser().getPosition())
                .title(t.getTitle())
                .description(t.getDescription())
                .deadline(t.getDeadline())
                .status(t.getStatus())
                .startedAt(t.getStartedAt())
                .completedAt(t.getCompletedAt())
                .spentMinutes(t.getSpentMinutes())
                .overdue(isOverdue)
                .build();
    }
}