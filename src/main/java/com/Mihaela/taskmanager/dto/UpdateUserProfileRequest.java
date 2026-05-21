package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.Role;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserProfileRequest {

    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    private Role role;
    private Boolean isActive;
}
