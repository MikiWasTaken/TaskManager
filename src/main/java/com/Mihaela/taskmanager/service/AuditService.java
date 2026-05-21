package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.entity.AuditLog;
import com.Mihaela.taskmanager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public List<AuditLog> findAll() {
        return auditLogRepository.findAll();
    }

    public void log(String action, String performedBy,
                    String entityType, UUID entityId, String details) {

        AuditLog entry = AuditLog.builder()
                .action(action)
                .performedBy(performedBy)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(entry);
        log.info("[AUDIT] {} by {} | entity={}#{} | {}",
                action, performedBy, entityType, entityId, details);
    }
}