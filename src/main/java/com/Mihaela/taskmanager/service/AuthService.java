package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.dto.AuthResponse;
import com.Mihaela.taskmanager.dto.LoginRequest;
import com.Mihaela.taskmanager.entity.User;
import com.Mihaela.taskmanager.exception.BadRequestException;
import com.Mihaela.taskmanager.exception.ResourceNotFoundException;
import com.Mihaela.taskmanager.exception.UnauthorizedException;
import com.Mihaela.taskmanager.repository.UserRepository;
import com.Mihaela.taskmanager.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthResponse login(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());

        if (userOpt.isEmpty()) {
            log.warn("Login failed: email not found '{}'", loginRequest.getEmail());
            auditService.log("USER_LOGIN_FAILED", loginRequest.getEmail(), "User", null, "Email not registered");
            throw new ResourceNotFoundException("User not found");
        }

        User user = userOpt.get();

        if (!user.getActive()) {
            log.warn("Login blocked: account deactivated for '{}'", user.getEmail());
            auditService.log("USER_LOGIN_FAILED", user.getEmail(), "User", user.getId(), "Account deactivated");
            throw new UnauthorizedException("This account is deactivated");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("Login failed: wrong password for '{}'", user.getEmail());
            auditService.log("USER_LOGIN_FAILED", user.getEmail(), "User", user.getId(), "Wrong password");
            throw new BadRequestException("Invalid credentials");
        }

        log.info("User '{}' logged in successfully", user.getEmail());
        auditService.log("USER_LOGIN", user.getEmail(), "User", user.getId(), "Login successful");

        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token);
    }
}
