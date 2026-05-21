package com.Mihaela.taskmanager.controller;

import com.Mihaela.taskmanager.dto.DocumentDownload;
import com.Mihaela.taskmanager.dto.DocumentResponse;
import com.Mihaela.taskmanager.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(201).body(documentService.uploadDocument(projectId, file));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable UUID projectId) {
        return ResponseEntity.ok(documentService.listDocuments(projectId));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {

        DocumentDownload download = documentService.downloadDocument(projectId, documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + download.filename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(download.stream()));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID projectId,
            @PathVariable UUID documentId) {
        documentService.deleteDocument(projectId, documentId);
        return ResponseEntity.noContent().build();
    }
}