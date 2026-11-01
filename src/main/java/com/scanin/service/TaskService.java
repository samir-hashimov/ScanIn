package com.scanin.service;

import com.scanin.dao.entity.EmployeeTask;
import com.scanin.dao.entity.User;
import com.scanin.dao.repository.EmployeeTaskRepository;
import com.scanin.dao.repository.UserRepository;
import com.scanin.dto.request.CreateAdHocActivityRequest;
import com.scanin.dto.request.CreateTaskRequest;
import com.scanin.dto.response.TaskResponse;
import com.scanin.exception.ResourceNotFoundException;
import com.scanin.util.TaskKind;
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
                .kind(TaskKind.ASSIGNED)
                .spentMinutes(0)
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse createAdHocActivity(CreateAdHocActivityRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("İstifadəçi tapılmadı!"));

        LocalDateTime now = LocalDateTime.now();
        EmployeeTask parent = null;

        if (request.getPausedTaskId() != null) {
            parent = getOwnedTask(request.getPausedTaskId(), username);
            if (parent.getStatus() == TaskStatus.IN_PROGRESS) {
                pauseInternal(parent, now);
            } else if (parent.getStatus() != TaskStatus.PAUSED) {
                throw new IllegalStateException("Yalnız icradakı və ya fasilədəki tapşırıqdan fəaliyyət açıla bilər!");
            }
        }

        pauseOtherActiveTasks(user, parent != null ? parent.getId() : null, now);

        int allocated = request.getAllocatedMinutes() != null && request.getAllocatedMinutes() > 0
                ? request.getAllocatedMinutes()
                : 60;

        EmployeeTask activity = EmployeeTask.builder()
                .user(user)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .deadline(now.plusMinutes(allocated))
                .status(TaskStatus.IN_PROGRESS)
                .kind(TaskKind.ADHOC)
                .spentMinutes(0)
                .allocatedMinutes(allocated)
                .startedAt(now)
                .lastResumedAt(now)
                .pausedFromTask(parent)
                .createdAt(now)
                .build();

        return toResponse(taskRepository.save(activity));
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
        LocalDateTime now = LocalDateTime.now();

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Bu tapşırıq artıq tamamlanıb!");
        }

        if (task.getStatus() == TaskStatus.IN_PROGRESS) {
            return toResponse(task);
        }

        pauseOtherActiveTasks(task.getUser(), task.getId(), now);

        if (task.getStatus() == TaskStatus.PAUSED) {
            resumeInternal(task, now);
        } else {
            if (task.getStartedAt() == null) {
                task.setStartedAt(now);
            }
            task.setLastResumedAt(now);
            task.setPausedAt(null);
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse pauseTask(Long taskId, String username) {
        EmployeeTask task = getOwnedTask(taskId, username);
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException("Yalnız icradakı tapşırığa fasilə verilə bilər!");
        }
        pauseInternal(task, LocalDateTime.now());
        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse resumeTask(Long taskId, String username) {
        EmployeeTask task = getOwnedTask(taskId, username);
        if (task.getStatus() != TaskStatus.PAUSED) {
            throw new IllegalStateException("Yalnız fasilədəki tapşırığa davam edilə bilər!");
        }
        pauseOtherActiveTasks(task.getUser(), task.getId(), LocalDateTime.now());
        resumeInternal(task, LocalDateTime.now());
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
        freezeActiveSegment(task, now);
        task.setCompletedAt(now);
        task.setPausedAt(null);
        task.setLastResumedAt(null);
        task.setStatus(TaskStatus.COMPLETED);

        int spent = task.getSpentMinutes() != null ? task.getSpentMinutes() : 0;
        task.setSpentMinutes(Math.max(1, spent));

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasksForHr() {
        return taskRepository.findAllByOrderByDeadlineAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void pauseOtherActiveTasks(User user, Long exceptTaskId, LocalDateTime now) {
        List<EmployeeTask> active = taskRepository.findAllByUserAndStatusIn(
                user, List.of(TaskStatus.IN_PROGRESS));
        for (EmployeeTask other : active) {
            if (exceptTaskId != null && exceptTaskId.equals(other.getId())) {
                continue;
            }
            pauseInternal(other, now);
            taskRepository.save(other);
        }
    }

    private void pauseInternal(EmployeeTask task, LocalDateTime now) {
        freezeActiveSegment(task, now);
        task.setPausedAt(now);
        task.setStatus(TaskStatus.PAUSED);
    }

    private void resumeInternal(EmployeeTask task, LocalDateTime now) {
        task.setPausedAt(null);
        task.setLastResumedAt(now);
        task.setStatus(TaskStatus.IN_PROGRESS);
    }

    private void freezeActiveSegment(EmployeeTask task, LocalDateTime now) {
        if (task.getLastResumedAt() != null && task.getStatus() == TaskStatus.IN_PROGRESS) {
            long add = Duration.between(task.getLastResumedAt(), now).toMinutes();
            int current = task.getSpentMinutes() != null ? task.getSpentMinutes() : 0;
            task.setSpentMinutes(current + (int) Math.max(0, add));
        }
        task.setLastResumedAt(null);
    }

    private int liveSpentMinutes(EmployeeTask t) {
        int spent = t.getSpentMinutes() != null ? t.getSpentMinutes() : 0;
        if (t.getStatus() == TaskStatus.IN_PROGRESS && t.getLastResumedAt() != null) {
            long add = Duration.between(t.getLastResumedAt(), LocalDateTime.now()).toMinutes();
            spent += (int) Math.max(0, add);
        }
        return spent;
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

        Long parentId = null;
        String parentTitle = null;
        if (t.getPausedFromTask() != null) {
            parentId = t.getPausedFromTask().getId();
            parentTitle = t.getPausedFromTask().getTitle();
        }

        return TaskResponse.builder()
                .id(t.getId())
                .employeeCode(t.getUser().getEmployeeCode())
                .employeeFullName(t.getUser().getFullName())
                .employeePosition(t.getUser().getPosition())
                .title(t.getTitle())
                .description(t.getDescription())
                .deadline(t.getDeadline())
                .status(t.getStatus())
                .kind(t.getKind() != null ? t.getKind() : TaskKind.ASSIGNED)
                .startedAt(t.getStartedAt())
                .completedAt(t.getCompletedAt())
                .pausedAt(t.getPausedAt())
                .lastResumedAt(t.getLastResumedAt())
                .spentMinutes(liveSpentMinutes(t))
                .allocatedMinutes(t.getAllocatedMinutes())
                .pausedFromTaskId(parentId)
                .pausedFromTaskTitle(parentTitle)
                .overdue(isOverdue)
                .build();
    }
}
