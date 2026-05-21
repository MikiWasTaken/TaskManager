package com.Mihaela.taskmanager.controller;

import com.Mihaela.taskmanager.dto.CreateProjectRequest;
import com.Mihaela.taskmanager.dto.ProjectResponse;
import com.Mihaela.taskmanager.dto.UpdateProjectRequest;
import com.Mihaela.taskmanager.dto.UserResponse;
import com.Mihaela.taskmanager.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> findAllProjectsIncludingDeleted() {
        return ResponseEntity.ok(projectService.findAllIncludingDeleted());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProjectResponse> deleteProject(@PathVariable UUID id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ProjectResponse> addProjectMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
        return ResponseEntity.ok(projectService.addMember(projectId, userId));
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<UserResponse>> getMembers(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getMembers(projectId));
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ProjectResponse> deleteProjectMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
        projectService.deleteProjectMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }


}