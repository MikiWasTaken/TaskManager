package com.Mihaela.taskmanager.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {
    private UUID id;
    private String name;
    private String type;
    private Long size;
    private String ownerUsername;
    private UUID projectId;
    private LocalDateTime uploadedAt;
}