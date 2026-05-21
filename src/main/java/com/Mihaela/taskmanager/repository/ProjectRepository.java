package com.Mihaela.taskmanager.repository;

import com.Mihaela.taskmanager.entity.Project;
import com.Mihaela.taskmanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByMembersContainingOrOwner(User member, User owner);
    List<Project> findByDeletedAtIsNull();
    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);
}