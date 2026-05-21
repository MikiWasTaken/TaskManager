package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.dto.CreateProjectRequest;
import com.Mihaela.taskmanager.dto.ProjectResponse;
import com.Mihaela.taskmanager.dto.UpdateProjectRequest;
import com.Mihaela.taskmanager.dto.UserResponse;
import com.Mihaela.taskmanager.entity.Project;
import com.Mihaela.taskmanager.entity.ProjectStatus;
import com.Mihaela.taskmanager.entity.User;
import com.Mihaela.taskmanager.exception.BadRequestException;
import com.Mihaela.taskmanager.exception.ResourceNotFoundException;
import com.Mihaela.taskmanager.exception.UnauthorizedException;
import com.Mihaela.taskmanager.repository.ProjectRepository;
import com.Mihaela.taskmanager.repository.UserRepository;
import com.Mihaela.taskmanager.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;
    private final AuditService auditService;


    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        User owner = authUtils.getCurrentUser();

        Project project = new Project();

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setOwner(owner);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        project.getMembers().add(owner);
        Project savedProject = projectRepository.save(project);
        log.info("User {} created project id={} name='{}'", owner.getEmail(), savedProject.getId(), savedProject.getName());
        auditService.log("PROJECT_CREATED", owner.getEmail(), "Project", savedProject.getId(), "Project created: " + savedProject.getName());
        return ProjectResponse.from(savedProject);
    }

    @Transactional
    public ProjectResponse getById(UUID id) {

        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = authUtils.getCurrentUser();

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser) || authUtils.isMember(project, currentUser)))
            throw new UnauthorizedException("Unauthorized access");

        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = authUtils.getCurrentUser();

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("PROJECT_UPDATE_FAILED", currentUser.getEmail(), "Project", project.getId(), "Unauthorized update attempt");
            throw new UnauthorizedException("Unauthorized access");
        }

        ProjectStatus originalStatus = project.getStatus();

        if (originalStatus == ProjectStatus.COMPLETE &&
                (request.getProjectStatus() == null || request.getProjectStatus() == ProjectStatus.COMPLETE)) {
            auditService.log("PROJECT_UPDATE_FAILED", currentUser.getEmail(), "Project", project.getId(), "Attempted to update a COMPLETE project");
            throw new BadRequestException("You cannot update completed projects. If you're the owner/admin, switch the project to active first");
        }

        if (request.getProjectStatus() != null) {
            if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser)))
                throw new UnauthorizedException("You are not allowed to change the project status");

            log.info("User {} changed project id={} status: {} -> {}",
                    currentUser.getEmail(), project.getId(), originalStatus, request.getProjectStatus());
            auditService.log("PROJECT_STATUS_UPDATED", currentUser.getEmail(), "Project", project.getId(),
                    "Status changed: " + originalStatus + " -> " + request.getProjectStatus());
            project.setStatus(request.getProjectStatus());
        }

        if (request.getName() != null) {

            if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser)))
                throw new UnauthorizedException("Members cannot change project names");

            if (request.getName().isBlank())
                throw new BadRequestException("Project name cannot be empty or blank");
            project.setName(request.getName());
        }

        if (request.getDescription() != null) {
            if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser)))
                throw new UnauthorizedException("Members cannot change project description");

            if (request.getDescription().isBlank())
                throw new BadRequestException("Project description cannot be blank");
            project.setDescription(request.getDescription());
        }


        if (request.getOwner() != null) {
            User newOwner = userRepository.findByIdAndActiveTrue(request.getOwner()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser)))
                throw new UnauthorizedException("You are not allowed to change ownership of this project");

            if (Objects.equals(project.getOwner().getId(), newOwner.getId()))
                throw new BadRequestException("New user is already the owner");

            if (!project.getMembers().contains(newOwner))
                throw new BadRequestException("New owner must be a member of this project");

//            if(!project.getMembers().contains(currentUser))
//                project.getMembers().add(currentUser);

            log.info("User {} transferred ownership of project id={} from {} to {}",
                    currentUser.getEmail(), project.getId(), project.getOwner().getEmail(), newOwner.getEmail());
            auditService.log("PROJECT_OWNERSHIP_TRANSFERRED", currentUser.getEmail(), "Project", project.getId(),
                    "Ownership transferred from " + project.getOwner().getEmail() + " to " + newOwner.getEmail());
            project.setOwner(newOwner);

            projectRepository.save(project);
            return ProjectResponse.from(project);
        }

        Project savedProject = projectRepository.save(project);
        auditService.log("PROJECT_UPDATED", currentUser.getEmail(), "Project", savedProject.getId(), "Project fields updated");
        return ProjectResponse.from(savedProject);
    }


    @Transactional
    public List<UserResponse> getMembers(UUID id) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = authUtils.getCurrentUser();

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser) || authUtils.isMember(project, currentUser)))
            throw new UnauthorizedException("Unauthorized access");

        return project.getMembers().stream()
                .map(UserResponse::from)
                .toList();
    }

    //for admin (no owner checking)
    @Transactional
    public void delete(UUID id) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = authUtils.getCurrentUser();

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser))) {
            auditService.log("PROJECT_DELETE_FAILED", currentUser.getEmail(), "Project", project.getId(), "Unauthorized delete attempt");
            throw new UnauthorizedException("You are not allowed to delete this project");
        }

        project.setDeletedAt(LocalDateTime.now());
        projectRepository.save(project);
        log.info("User {} deleted project id={} name='{}'", currentUser.getEmail(), project.getId(), project.getName());
        auditService.log("PROJECT_DELETE", currentUser.getEmail(),
                "Project", project.getId(), "Soft deleted project: " + project.getName());
    }


    @Transactional
    public ProjectResponse addMember(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (project.getStatus() == ProjectStatus.COMPLETE)
            throw new BadRequestException("You cannot add members to a complete project");

        User addedUser = userRepository.findByIdAndActiveTrue(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User currentUser = authUtils.getCurrentUser();

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("PROJECT_MEMBER_ADD_FAILED", currentUser.getEmail(), "Project", projectId, "Unauthorized: tried to add " + addedUser.getEmail());
            throw new UnauthorizedException("You are not allowed to add members to this project");
        }

        if (project.getMembers().contains(addedUser))
            throw new BadRequestException("User is already a member of this project");

        project.getMembers().add(addedUser);
        log.info("User {} added member {} to project id={}", currentUser.getEmail(), addedUser.getEmail(), projectId);
        auditService.log("PROJECT_MEMBER_ADDED", currentUser.getEmail(), "Project", projectId, "Member added: " + addedUser.getEmail());

        Project savedProject = projectRepository.save(project);
        return ProjectResponse.from(savedProject);
    }

    @Transactional
    public void deleteProjectMember(UUID projectId, UUID userId) {

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (project.getStatus() == ProjectStatus.COMPLETE)
            throw new BadRequestException("You cannot delete members from a complete project");


        //don't check if they are active. Maybe they got added to the project and got deactivated. You'd still want to delete
        User removedMember = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (removedMember.getId().equals(project.getOwner().getId()))
            throw new UnauthorizedException("Cannot delete the member who is also the owner of this project");

        User currentUser = authUtils.getCurrentUser();

        // members can add other members, but can't delete them

        if (authUtils.isMember(project, currentUser)) {
            if (currentUser.equals(removedMember)) {
                project.getMembers().remove(removedMember);
                projectRepository.save(project);
                log.info("User {} left project id={}", currentUser.getEmail(), projectId);
                auditService.log("PROJECT_MEMBER_LEFT", currentUser.getEmail(), "Project", projectId, "User left the project");
                return;
            }
        }

        if (!(authUtils.isAdmin(currentUser) || authUtils.isOwner(project, currentUser))) {
            auditService.log("PROJECT_MEMBER_REMOVE_FAILED", currentUser.getEmail(), "Project", projectId,
                    "Unauthorized: tried to remove " + removedMember.getEmail());
            throw new UnauthorizedException("You are not allowed to delete members of this project.");
        }

        if (!project.getMembers().contains(removedMember))
            throw new BadRequestException("User is not a member of this project");

        project.getMembers().remove(removedMember);
        projectRepository.save(project);
        log.info("User {} removed member {} from project id={}", currentUser.getEmail(), removedMember.getEmail(), projectId);
        auditService.log("PROJECT_MEMBER_REMOVED", currentUser.getEmail(), "Project", projectId, "Member removed: " + removedMember.getEmail());
    }

    //for admins
    public List<ProjectResponse> findAllIncludingDeleted() {
        return projectRepository.findAll().stream().map(ProjectResponse::fromWithDeleted).toList();
    }


}