package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.Task;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class TaskResponse {

    private UUID id;
    private String title;
    private String description;
    private String priority;
    private String status;

    private LocalDateTime createdAt;

    private UUID projectId;
    private UUID assignee;
    private UUID createdBy;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private LocalDateTime deadline;

    public static TaskResponse from(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority().name());
        response.setStatus(task.getStatus().name());
        response.setCreatedAt(task.getCreatedAt());
        response.setProjectId(task.getProject().getId());
        response.setCreatedBy(task.getCreatedBy().getId());
        response.setUpdatedAt(task.getUpdatedAt());
        response.setDeadline(task.getDeadline());

        if (task.getAssignedTo() != null)
            response.setAssignee(task.getAssignedTo().getId());

        return response;
    }


    public static TaskResponse fromWithDeleted(Task task) {
        TaskResponse response = TaskResponse.from(task);
        response.setDeletedAt(task.getDeletedAt());

        if (task.getAssignedTo() != null)
            response.setAssignee(task.getAssignedTo().getId());

        return response;
    }
}