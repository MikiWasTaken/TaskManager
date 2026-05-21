package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.TaskPriority;
import com.Mihaela.taskmanager.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UpdateTaskRequest {

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;

    @Size(min = 1, max = 5000, message = "Description must be between 1 and 5000 characters")
    private String description;

    private TaskStatus status;
    private TaskPriority priority;
    private UUID assigneeId;

    @FutureOrPresent(message = "Deadline must be in the present or future")
    private LocalDateTime deadline;
}
