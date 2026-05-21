package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.dto.*;
import com.Mihaela.taskmanager.entity.Project;
import com.Mihaela.taskmanager.entity.Role;
import com.Mihaela.taskmanager.entity.User;
import com.Mihaela.taskmanager.exception.BadRequestException;
import com.Mihaela.taskmanager.exception.ResourceNotFoundException;
import com.Mihaela.taskmanager.exception.UnauthorizedException;
import com.Mihaela.taskmanager.repository.ProjectRepository;
import com.Mihaela.taskmanager.repository.UserRepository;
import com.Mihaela.taskmanager.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final AuthUtils authUtils;
    private final AuditService auditService;


    public UserResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            auditService.log("USER_REGISTER_FAILED", request.getEmail(), "User", null, "Email already in use");
            throw new BadRequestException("Email already in use");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = userRepository.save(user);
        log.info("New user registered: id={} email='{}'", savedUser.getId(), savedUser.getEmail());
        auditService.log("USER_REGISTER", savedUser.getUsername(), "User", savedUser.getId(), "New user registered");
        return UserResponse.from(savedUser);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found: " + email));

        return user;
    }

    public UserResponse getCurrentUserProfile() {
        User user = authUtils.getCurrentUser();

        if (user == null)
            throw new UnauthorizedException("Something went wrong. Please log in again");

        return UserResponse.from(user);
    }

    public UserResponse updateOwnProfile(UpdateOwnProfileRequest request) {

        User user = authUtils.getCurrentUser();

        if (user == null)
            throw new UnauthorizedException("Something went wrong. Please log in again");


        if (request.getFirstName() != null) {
            if (request.getFirstName().isBlank())
                throw new BadRequestException("First name cannot be empty");
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            if (request.getLastName().isBlank())
                throw new BadRequestException("Last name cannot be empty");
            user.setLastName(request.getLastName());
        }

        if (request.getFirstName() != null || request.getLastName() != null) {
            auditService.log("USER_PROFILE_UPDATED", user.getEmail(), "User", user.getId(), "Name updated");
        }

        if (request.getIsActive() != null) {
            if (!request.getIsActive() && user.getRole() == Role.ADMIN) {
                if (userRepository.findAllByRole(Role.ADMIN).size() == 1)
                    throw new BadRequestException("You are the only current admin. Cannot deactivate this account");
            }

            if (request.getIsActive())
                throw new BadRequestException("You are already active");

            log.info("User '{}' deactivated their own account", user.getEmail());
            auditService.log("USER_SELF_DEACTIVATED", user.getEmail(), "User", user.getId(), "User deactivated own account");
            user.setActive(false);
        }

        // change password
        if (request.getCurrentPassword() != null) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword()))
                throw new BadRequestException("Invalid current password");

            if (request.getNewPassword() == null || request.getNewPassword().isBlank())
                throw new BadRequestException("New password is required");

            log.info("User '{}' changed their password", user.getEmail());
            auditService.log("USER_PASSWORD_CHANGED", user.getEmail(), "User", user.getId(), "Password changed");
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return UserResponse.from(user);
    }

    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();

    }

    public UserResponse findUserById(UUID userId)
    {
        User user = userRepository.findById(userId).orElseThrow (() -> new ResourceNotFoundException("User not found"));

        return UserResponse.from(user);
    }

    public List<Project> getCurrentUserProjects() {
        User user = authUtils.getCurrentUser();
        return projectRepository.findByMembersContainingOrOwner(user, user);
    }


    // update user as admin
    public UserResponse updateUser(UUID userId, UpdateUserProfileRequest request)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            if (request.getFirstName().isBlank())
                throw new BadRequestException("First name cannot be empty");

            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            if (request.getLastName().isBlank())
                throw new BadRequestException("Last name cannot be empty");
            user.setLastName(request.getLastName());
        }

        if(request.getRole() != null)
        {
            if (request.getRole() == Role.USER && user.getRole() == Role.ADMIN)
            {
                if (userRepository.findAllByRole(Role.ADMIN).size() == 1)
                    throw new BadRequestException("You are the only current admin. Cannot change role to user");
            }

            if (user.getRole() == Role.USER && request.getRole() == Role.USER)
                throw new BadRequestException("User already has role user");

            if (user.getRole() == Role.ADMIN && request.getRole() == Role.ADMIN)
                throw new BadRequestException("User already has role admin");

            log.info("Admin changed role of user id={} email='{}': {} -> {}",
                    user.getId(), user.getEmail(), user.getRole(), request.getRole());
            auditService.log("USER_ROLE_UPDATED", authUtils.getCurrentUser().getEmail(),
                    "User", user.getId(), "Role changed: " + user.getRole() + " -> " + request.getRole());
            user.setRole(request.getRole());
        }

        if(request.getIsActive() != null)
        {
            if (!request.getIsActive() && user.getRole() == Role.ADMIN)
            {
                if (userRepository.findAllByRole(Role.ADMIN).size() == 1)
                    throw new BadRequestException("You are the only current admin. Cannot deactivate this account");
            }

            if(request.getIsActive() && user.getActive() == true)
            {
                throw new BadRequestException("User is already active");
            }
            if(!request.getIsActive() && user.getActive() == false)
            {
                throw new BadRequestException("User is already deactivated");
            }
            log.info("Admin set active={} for user id={} email='{}'", request.getIsActive(), user.getId(), user.getEmail());
            String action = request.getIsActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED";
            String details = request.getIsActive() ? "Account activated by admin" : "Account deactivated by admin";
            auditService.log(action, authUtils.getCurrentUser().getEmail(), "User", user.getId(), details);
            user.setActive(request.getIsActive());
        }

        userRepository.save(user);

        return UserResponse.from(user);


    }
}