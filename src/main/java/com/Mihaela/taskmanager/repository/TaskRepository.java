package com.Mihaela.taskmanager.repository;

import com.Mihaela.taskmanager.entity.Task;
import com.Mihaela.taskmanager.entity.TaskPriority;
import com.Mihaela.taskmanager.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByProjectId(UUID projectId);
    Optional<Task> findByIdAndDeletedAtIsNull(UUID id);

    List<Task> findByProjectIdAndDeletedAtIsNull(UUID projectId);
    List<Task> findByProjectIdAndStatusAndDeletedAtIsNull(UUID projectId, TaskStatus status);
    List<Task> findByProjectIdAndPriorityAndDeletedAtIsNull(UUID projectId, TaskPriority priority);
    List<Task> findByProjectIdAndStatusAndPriorityAndDeletedAtIsNull(UUID projectId, TaskStatus status, TaskPriority priority);

    List<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status);
    List<Task> findByProjectIdAndPriority(UUID projectId, TaskPriority priority);
    List<Task> findByProjectIdAndStatusAndPriority(UUID projectId, TaskStatus status, TaskPriority priority);
}