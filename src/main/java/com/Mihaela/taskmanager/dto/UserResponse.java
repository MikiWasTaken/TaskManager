package com.Mihaela.taskmanager.dto;

import com.Mihaela.taskmanager.entity.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().name());
        response.setActive(user.isEnabled());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
