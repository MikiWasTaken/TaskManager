package com.Mihaela.taskmanager.service;

import com.Mihaela.taskmanager.dto.DocumentDownload;
import com.Mihaela.taskmanager.dto.DocumentResponse;
import com.Mihaela.taskmanager.entity.Document;
import com.Mihaela.taskmanager.entity.Project;
import com.Mihaela.taskmanager.entity.User;
import com.Mihaela.taskmanager.exception.BadRequestException;
import com.Mihaela.taskmanager.exception.ResourceNotFoundException;
import com.Mihaela.taskmanager.repository.DocumentRepository;
import com.Mihaela.taskmanager.repository.ProjectRepository;
import com.Mihaela.taskmanager.security.AuthUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final MinioService minioService;
    private final AuthUtils authUtils;
    private final AuditService auditService;


    public DocumentResponse uploadDocument(UUID projectId, MultipartFile file) {
        User currentUser = authUtils.getCurrentUser();

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("DOCUMENT_UPLOAD_FAILED", currentUser.getEmail(), "Project", projectId, "Unauthorized upload attempt");
            throw new BadRequestException("You are not allowed to upload documents in this project");
        }

        // upload to MinIO, get back the object key
        String objectKey = minioService.uploadFile(file, projectId);

        // save metadata in PostgreSQL
        Document document = Document.builder()
                .name(file.getOriginalFilename())
                .type(file.getContentType())
                .size(file.getSize())
                .objectKey(objectKey)
                .project(project)
                .owner(currentUser)
                .build();

        document = documentRepository.save(document);
        log.info("User {} uploaded document '{}' to project {}",
                currentUser.getUsername(), file.getOriginalFilename(), projectId);
        auditService.log("DOCUMENT_UPLOAD", currentUser.getUsername(),
                "Document", document.getId(), "File: " + file.getOriginalFilename());


        return toResponse(document);
    }

    public DocumentDownload downloadDocument(UUID projectId, UUID documentId) {
        User currentUser = authUtils.getCurrentUser();

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("DOCUMENT_DOWNLOAD_FAILED", currentUser.getEmail(), "Document", documentId, "Unauthorized download attempt");
            throw new BadRequestException("You are not allowed to download documents from this project");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (!document.getProject().getId().equals(projectId))
            throw new BadRequestException("Document does not belong to this project");

        auditService.log("DOCUMENT_DOWNLOAD", currentUser.getEmail(), "Document", document.getId(), "File downloaded: " + document.getName());
        return new DocumentDownload(minioService.downloadFile(document.getObjectKey()), document.getName());
    }

    public List<DocumentResponse> listDocuments(UUID projectId) {
        User currentUser = authUtils.getCurrentUser();

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser)))
            throw new BadRequestException("You are not allowed to download documents from this project");


        return documentRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteDocument(UUID projectId, UUID documentId) {

        User currentUser = authUtils.getCurrentUser();

        Project project = projectRepository.findByIdAndDeletedAtIsNull(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (!(authUtils.isAdmin(currentUser) || authUtils.isMember(project, currentUser))) {
            auditService.log("DOCUMENT_DELETE_FAILED", currentUser.getEmail(), "Document", documentId, "Unauthorized delete attempt");
            throw new BadRequestException("You are not allowed to delete documents in this project");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        auditService.log("DOCUMENT_DELETE", authUtils.getCurrentUser().getUsername(),
                "Document", documentId, "File: " + document.getName());

        minioService.deleteFile(document.getObjectKey());
        documentRepository.delete(document);

        log.info("Deleted document id={} name='{}'", documentId, document.getName());
    }

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .name(doc.getName())
                .type(doc.getType())
                .size(doc.getSize())
                .ownerUsername(doc.getOwner().getUsername())
                .projectId(doc.getProject().getId())
                .uploadedAt(doc.getUploadedAt())
                .build();
    }
}