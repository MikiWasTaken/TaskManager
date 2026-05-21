package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.TaskPriority;
import com.Mihaela.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @FutureOrPresent(message = "Deadline must be in the present or future")
    private LocalDateTime deadline;

    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;
}
