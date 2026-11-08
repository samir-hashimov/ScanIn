package com.scanin.controller;

import com.scanin.dto.request.CompleteTaskRequest;
import com.scanin.dto.request.CreateAdHocActivityRequest;
import com.scanin.dto.request.CreateTaskRequest;
import com.scanin.dto.response.TaskResponse;
import com.scanin.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // 1. İŞÇİ ÜÇÜN: Öz tapşırıqlarını görmək (Masaüstü QR oxudanda):
    @GetMapping("/tasks/my")
    public ResponseEntity<List<TaskResponse>> getMyTasks(Authentication authentication) {
        return ResponseEntity.ok(taskService.getMyTasks(authentication.getName()));
    }

    // 2. İŞÇİ ÜÇÜN: Tapşırığa BAŞLA:
    @PostMapping("/tasks/{id}/start")
    public ResponseEntity<TaskResponse> startTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.startTask(id, authentication.getName()));
    }

    // 3. İŞÇİ ÜÇÜN: Tapşırığı BİTİR:
    @PostMapping({"/tasks/{id}/complete", "/tasks/{id}/finish"})
    public ResponseEntity<TaskResponse> completeTask(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) String completionComment,
            @RequestBody(required = false) CompleteTaskRequest body) {
        String note = firstNonBlank(
                comment,
                completionComment,
                body != null ? body.getComment() : null,
                body != null ? body.getCompletionComment() : null
        );
        return ResponseEntity.ok(taskService.completeTask(id, authentication.getName(), note));
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    @PostMapping("/tasks/{id}/pause")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.pauseTask(id, authentication.getName()));
    }

    @PostMapping("/tasks/{id}/resume")
    public ResponseEntity<TaskResponse> resumeTask(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(taskService.resumeTask(id, authentication.getName()));
    }

    @PostMapping("/tasks/ad-hoc")
    public ResponseEntity<TaskResponse> createAdHocActivity(
            @Valid @RequestBody CreateAdHocActivityRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(taskService.createAdHocActivity(request, authentication.getName()));
    }

    // 4. HR VƏ 1C ÜÇÜN: İşçiyə yeni tapşırıq təyin etmək:
    @PostMapping("/hr/tasks")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.ok(taskService.createOrSyncTask(request));
    }

    // 5. HR ÜÇÜN: Bütün işçilərin fəaliyyət və tapşırıqlarını izləmək:
    @GetMapping("/hr/tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasksForHr() {
        return ResponseEntity.ok(taskService.getAllTasksForHr());
    }
}