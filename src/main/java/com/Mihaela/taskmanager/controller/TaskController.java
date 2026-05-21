package com.Mihaela.taskmanager.controller;

import com.Mihaela.taskmanager.dto.CreateTaskRequest;
import com.Mihaela.taskmanager.dto.TaskResponse;
import com.Mihaela.taskmanager.dto.UpdateTaskRequest;
import com.Mihaela.taskmanager.entity.TaskPriority;
import com.Mihaela.taskmanager.entity.TaskStatus;
import com.Mihaela.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus taskStatus,
            @RequestParam(required = false) TaskPriority taskPriority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore) {
        return ResponseEntity.ok(taskService.findAllInProject(projectId, taskStatus, taskPriority, deadlineAfter, deadlineBefore, createdAfter, createdBefore));
    }


    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@PathVariable UUID projectId, @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse task = taskService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }


    @GetMapping("/{taskId}")
    public ResponseEntity<TaskResponse> getById(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        return ResponseEntity.ok(taskService.findById(projectId, taskId));
    }


    @PatchMapping("/{taskId}")
    public ResponseEntity<TaskResponse> update(@PathVariable UUID projectId, @PathVariable UUID taskId,
                                               @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(projectId, taskId, request));

    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<TaskResponse> deleteTaskById(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(projectId, taskId);
        return ResponseEntity.noContent().build();
    }
}