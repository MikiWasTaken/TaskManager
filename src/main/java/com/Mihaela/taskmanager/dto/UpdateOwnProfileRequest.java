package com.Mihaela.taskmanager.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOwnProfileRequest {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @Size(max = 128, message = "Password must not exceed 128 characters")
    private String currentPassword;

    private Boolean isActive;

    @Size(min = 6, max = 64, message = "New password must be between 6 and 64 characters")
    private String newPassword;
}
