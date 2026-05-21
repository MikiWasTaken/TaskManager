package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.ProjectStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UpdateProjectRequest {

    @Size(min = 1, max = 255, message = "Project name must be between 1 and 255 characters")
    private String name;

    @Size(min = 1, max = 2000, message = "Description must be between 1 and 2000 characters")
    private String description;

    private UUID owner;
    private ProjectStatus projectStatus;
}
