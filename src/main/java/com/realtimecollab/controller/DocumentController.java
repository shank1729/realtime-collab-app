package com.realtimecollab.controller;

import com.realtimecollab.dto.DocumentPatchRequest;
import com.realtimecollab.dto.DocumentRequest;
import com.realtimecollab.dto.DocumentResponse;
import com.realtimecollab.dto.DocumentVersionResponse;
import com.realtimecollab.dto.ShareDocumentRequest;
import com.realtimecollab.mapper.DocumentMapper;
import com.realtimecollab.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
@CrossOrigin(origins = "http://localhost:3000")
public class DocumentController {
    private final DocumentService service;
    private final DocumentMapper mapper;

    public DocumentController(DocumentService service, DocumentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAll(Authentication authentication) {
        List<DocumentResponse> responses = service.getAll(authentication.getName()).stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getOne(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(service.getById(id, authentication.getName())));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DocumentVersionResponse>> getHistory(
            @PathVariable Long id,
            Authentication authentication) {
        List<DocumentVersionResponse> history = service.getVersionHistory(id, authentication.getName())
                .stream()
                .map(mapper::toVersionResponse)
                .toList();
        return ResponseEntity.ok(history);
    }

    @GetMapping("/me/primary")
    public ResponseEntity<DocumentResponse> getPrimaryDocument(Authentication authentication) {
        return ResponseEntity.ok(
                mapper.toResponse(service.getFirstOrCreateStarter(authentication.getName()))
        );
    }

    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapper.toResponse(service.create(mapper.toEntity(request), authentication.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                mapper.toResponse(service.update(id, mapper.toEntity(request), authentication.getName()))
        );
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<DocumentResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody DocumentPatchRequest partialRequest,
            Authentication authentication) {
        return ResponseEntity.ok(
                mapper.toResponse(service.patch(id, partialRequest, authentication.getName()))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        service.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<DocumentResponse> shareDocument(
            @PathVariable Long id,
            @Valid @RequestBody ShareDocumentRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(
                mapper.toResponse(service.shareDocument(id, request, authentication.getName()))
        );
    }
}
