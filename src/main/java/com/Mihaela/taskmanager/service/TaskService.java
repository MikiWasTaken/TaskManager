package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.dto.CreateTaskRequest;
import com.Mihaela.taskmanager.dto.TaskResponse;
import com.Mihaela.taskmanager.dto.UpdateTaskRequest;
import com.Mihaela.taskmanager.entity.*;
import com.Mihaela.taskmanager.exception.BadRequestException;
import com.Mihaela.taskmanager.exception.ResourceNotFoundException;
import com.Mihaela.taskmanager.exception.UnauthorizedException;
import com.Mihaela.taskmanager.repository.ProjectRepository;
import com.Mihaela.taskmanager.repository.TaskRepository;
import com.Mihaela.taskmanager.repository.UserRepository;
import com.Mihaela.taskmanager.security.AuthUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final AuthUtils authUtils;
    private final AuditService auditService;

    @Transactional
    public TaskResponse create(UUID projectId, CreateTaskRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId).orElseThrow(() ->
                new ResourceNotFoundException("Project not found"));

        if (project.getStatus() == ProjectStatus.COMPLETE)
            throw new BadRequestException("Cannot add tasks to completed projects");

        User currentUser = authUtils.getCurrentUser();
        if (!(authUtils.isOwner(project, currentUser) || authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("TASK_CREATE_FAILED", currentUser.getEmail(), "Project", projectId, "Unauthorized: not a project member");
            throw new UnauthorizedException("You are not allowed to add tasks in this project");
        }

        Task task = new Task();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User to be assigned not found"));

            if (!project.getMembers().contains(assignee)) {
                auditService.log("TASK_CREATE_FAILED", currentUser.getEmail(), "Project", projectId,
                        "Assigned user " + request.getAssigneeId() + " is not a project member");
                throw new BadRequestException("Cannot assign user that is not member of the project");
            }

            task.setAssignedTo(assignee);
        }

        if (request.getDeadline() != null)
            task.setDeadline(request.getDeadline());

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setProject(project);
        task.setCreatedBy(currentUser);

        Task savedTask = taskRepository.save(task);
        log.info("User '{}' created task id={} '{}' in project id={}", currentUser.getEmail(), savedTask.getId(), savedTask.getTitle(), projectId);
        auditService.log("TASK_CREATED", currentUser.getEmail(), "Task", savedTask.getId(), "Task created: " + savedTask.getTitle());
        return TaskResponse.from(savedTask);
    }

    @Transactional
    public List<TaskResponse> findAllInProject(UUID projectId, TaskStatus status, TaskPriority priority,
                                               LocalDateTime deadlineAfter, LocalDateTime deadlineBefore,
                                               LocalDateTime createdAfter, LocalDateTime createdBefore) {
        User currentUser = authUtils.getCurrentUser();

        Project project = authUtils.isAdmin(currentUser)
                ? projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"))
                : projectRepository.findByIdAndDeletedAtIsNull(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isOwner(project, currentUser) || authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser)))
            throw new UnauthorizedException("You are not allowed to view tasks in this project");

        List<Task> tasks;

        if (authUtils.isOwner(project, currentUser) || authUtils.isMember(project, currentUser)) {
            if (status != null && priority != null)
                tasks = taskRepository.findByProjectIdAndStatusAndPriorityAndDeletedAtIsNull(projectId, status, priority);
            else if (status != null)
                tasks = taskRepository.findByProjectIdAndStatusAndDeletedAtIsNull(projectId, status);
            else if (priority != null)
                tasks = taskRepository.findByProjectIdAndPriorityAndDeletedAtIsNull(projectId, priority);
            else
                tasks = taskRepository.findByProjectIdAndDeletedAtIsNull(projectId);

            return applyFilters(tasks, deadlineAfter, deadlineBefore, createdAfter, createdBefore).stream()
                    .map(TaskResponse::from)
                    .toList();
        } else {
            // admin sees soft-deleted tasks too
            if (status != null && priority != null)
                tasks = taskRepository.findByProjectIdAndStatusAndPriority(projectId, status, priority);
            else if (status != null)
                tasks = taskRepository.findByProjectIdAndStatus(projectId, status);
            else if (priority != null)
                tasks = taskRepository.findByProjectIdAndPriority(projectId, priority);
            else
                tasks = taskRepository.findByProjectId(projectId);

            return applyFilters(tasks, deadlineAfter, deadlineBefore, createdAfter, createdBefore).stream()
                    .map(TaskResponse::fromWithDeleted)
                    .toList();
        }
    }

    @Transactional
    public TaskResponse findById(UUID projectId, UUID taskId) {
        User currentUser = authUtils.getCurrentUser();

        Project project = authUtils.isAdmin(currentUser)
                ? projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"))
                : projectRepository.findByIdAndDeletedAtIsNull(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isOwner(project, currentUser) || authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser)))
            throw new UnauthorizedException("You are not allowed to view tasks in this project");

        Task task = authUtils.isAdmin(currentUser)
                ? taskRepository.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"))
                : taskRepository.findByIdAndDeletedAtIsNull(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getProject().getId().equals(project.getId()))
            throw new ResourceNotFoundException("Task not found");

        return authUtils.isAdmin(currentUser) ? TaskResponse.fromWithDeleted(task) : TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(UUID projectId, UUID taskId, UpdateTaskRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getProject().getId().equals(project.getId()))
            throw new ResourceNotFoundException("Task not found");

        if (project.getStatus() == ProjectStatus.COMPLETE)
            throw new BadRequestException("Cannot update tasks in completed projects");

        User currentUser = authUtils.getCurrentUser();
        if (!(authUtils.isOwner(project, currentUser) || authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("TASK_UPDATE_FAILED", currentUser.getEmail(), "Task", taskId, "Unauthorized update attempt");
            throw new UnauthorizedException("You are not allowed to modify tasks in this project");
        }

        if (request.getTitle() != null) {
            if (request.getTitle().isBlank())
                throw new BadRequestException("Title cannot be empty");
            task.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            if (request.getDescription().isBlank())
                throw new BadRequestException("Description cannot be empty");
            task.setDescription(request.getDescription());
        }

        if (request.getPriority() != null)
            task.setPriority(request.getPriority());

        if (request.getStatus() != null) {
            log.info("User '{}' changed task id={} status: {} -> {}", currentUser.getEmail(), task.getId(), task.getStatus(), request.getStatus());
            task.setStatus(request.getStatus());
        }

        if (request.getDeadline() != null)
            task.setDeadline(request.getDeadline());

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findByIdAndActiveTrue(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!project.getMembers().contains(assignee)) {
                auditService.log("TASK_UPDATE_FAILED", currentUser.getEmail(), "Task", taskId,
                        "Assigned user " + request.getAssigneeId() + " is not a project member");
                throw new UnauthorizedException("Only members of the project can be assigned to tasks");
            }

            if (task.getAssignedTo() != null && request.getAssigneeId().equals(task.getAssignedTo().getId()))
                throw new BadRequestException("New assignee is the same as the current assignee");

            task.setAssignedTo(assignee);
        }

        Task savedTask = taskRepository.save(task);
        auditService.log("TASK_UPDATED", currentUser.getEmail(), "Task", savedTask.getId(), "Task updated: " + savedTask.getTitle());
        return TaskResponse.from(savedTask);
    }

    public void delete(UUID projectId, UUID taskId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Task task = taskRepository.findByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!task.getProject().getId().equals(project.getId()))
            throw new ResourceNotFoundException("Task not found");

        if (project.getStatus() == ProjectStatus.COMPLETE)
            throw new BadRequestException("Cannot delete tasks in completed projects");

        User currentUser = authUtils.getCurrentUser();
        if (!(authUtils.isOwner(project, currentUser) || authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("TASK_DELETE_FAILED", currentUser.getEmail(), "Task", taskId, "Unauthorized delete attempt");
            throw new UnauthorizedException("You are not allowed to delete tasks in this project");
        }

        task.setDeletedAt(LocalDateTime.now());
        taskRepository.save(task);
        log.info("User '{}' deleted task id={} '{}' from project id={}", currentUser.getEmail(), task.getId(), task.getTitle(), projectId);
        auditService.log("TASK_DELETED", currentUser.getEmail(), "Task", task.getId(), "Task deleted: " + task.getTitle());
    }

    private List<Task> applyFilters(List<Task> tasks,
                                    LocalDateTime deadlineAfter, LocalDateTime deadlineBefore,
                                    LocalDateTime createdAfter, LocalDateTime createdBefore) {
        return tasks.stream()
                .filter(t -> deadlineAfter == null  || (t.getDeadline()   != null && !t.getDeadline().isBefore(deadlineAfter)))
                .filter(t -> deadlineBefore == null || (t.getDeadline()   != null && !t.getDeadline().isAfter(deadlineBefore)))
                .filter(t -> createdAfter == null   || (t.getCreatedAt()  != null && !t.getCreatedAt().isBefore(createdAfter)))
                .filter(t -> createdBefore == null  || (t.getCreatedAt()  != null && !t.getCreatedAt().isAfter(createdBefore)))
                .toList();
    }
}
