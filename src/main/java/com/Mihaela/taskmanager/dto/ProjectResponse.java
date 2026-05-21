package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectResponse {
    private UUID id;
    private String name;
    private String description;
    private String status;
    private UUID ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ProjectResponse from(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setStatus(project.getStatus().name());
        response.setDescription(project.getDescription());
        response.setOwnerId(project.getOwner().getId());
        response.setCreatedAt(project.getCreatedAt());
        response.setUpdatedAt(project.getUpdatedAt());

        return response;
    }

    public static ProjectResponse fromWithDeleted(Project project) {
        ProjectResponse response = from(project);
        response.setDeletedAt(project.getDeletedAt());
        return response;
    }
}