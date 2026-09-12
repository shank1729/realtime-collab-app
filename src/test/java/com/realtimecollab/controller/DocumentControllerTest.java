package com.realtimecollab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtimecollab.dto.DocumentRequest;
import com.realtimecollab.dto.ShareDocumentRequest;
import com.realtimecollab.dto.auth.AuthResponse;
import com.realtimecollab.dto.auth.RegisterRequest;
import com.realtimecollab.entity.DocumentRole;
import com.realtimecollab.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Test
    void documentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDocumentReturnsCreatedDocumentForAuthenticatedUser() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");

        mockMvc.perform(post("/documents")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                documentRequest("Interview Plan", "Initial draft"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Interview Plan"))
                .andExpect(jsonPath("$.content").value("Initial draft"))
                .andExpect(jsonPath("$.ownerEmail").value("owner@example.com"))
                .andExpect(jsonPath("$.version").isNumber());
    }

    @Test
    void createDocumentReturnsValidationErrorsForBlankTitleAndContent() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");

        mockMvc.perform(post("/documents")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentRequest("", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.title").value("Title is required"))
                .andExpect(jsonPath("$.validationErrors.content").value("Content is required"));
    }

    @Test
    void ownerCanShareDocumentWithViewerRole() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");
        register("Viewer", "viewer@example.com");
        int documentId = createDocument(owner, "Project Notes", "Private draft");

        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setEmail("viewer@example.com");
        request.setRole(DocumentRole.VIEWER);

        mockMvc.perform(post("/documents/{id}/share", documentId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaborators", hasSize(1)))
                .andExpect(jsonPath("$.collaborators[0].email").value("viewer@example.com"))
                .andExpect(jsonPath("$.collaborators[0].role").value("VIEWER"));
    }

    @Test
    void ownerCanPromoteViewerToEditorWithJsonRole() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");
        register("Teammate", "teammate@example.com");
        int documentId = createDocument(owner, "Project Notes", "Private draft");
        shareDocument(owner, documentId, "teammate@example.com", DocumentRole.VIEWER);

        mockMvc.perform(post("/documents/{id}/share", documentId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"teammate@example.com\",\"role\":\"EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collaborators", hasSize(1)))
                .andExpect(jsonPath("$.collaborators[0].email").value("teammate@example.com"))
                .andExpect(jsonPath("$.collaborators[0].role").value("EDITOR"));
    }

    @Test
    void viewerCanReadButCannotUpdateDocument() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");
        AuthResponse viewer = register("Viewer", "viewer@example.com");
        int documentId = createDocument(owner, "Project Notes", "Private draft");
        shareDocument(owner, documentId, "viewer@example.com", DocumentRole.VIEWER);

        mockMvc.perform(get("/documents/{id}", documentId)
                        .header("Authorization", bearer(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Private draft"));

        mockMvc.perform(put("/documents/{id}", documentId)
                        .header("Authorization", bearer(viewer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                documentRequest("Project Notes", "Changed by viewer"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only document owners and editors can edit this document."));
    }

    @Test
    void editorCanPatchSharedDocumentAndHistoryIsReturned() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");
        AuthResponse editor = register("Editor", "editor@example.com");
        int documentId = createDocument(owner, "Project Notes", "Initial draft");
        shareDocument(owner, documentId, "editor@example.com", DocumentRole.EDITOR);

        mockMvc.perform(patch("/documents/{id}", documentId)
                        .header("Authorization", bearer(editor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated by editor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Updated by editor"))
                .andExpect(jsonPath("$.lastEditedByEmail").value("editor@example.com"));

        mockMvc.perform(get("/documents/{id}/history", documentId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void onlyOwnerCanDeleteDocument() throws Exception {
        AuthResponse owner = register("Owner", "owner@example.com");
        AuthResponse editor = register("Editor", "editor@example.com");
        int documentId = createDocument(owner, "Project Notes", "Initial draft");
        shareDocument(owner, documentId, "editor@example.com", DocumentRole.EDITOR);

        mockMvc.perform(delete("/documents/{id}", documentId)
                        .header("Authorization", bearer(editor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only the document owner can delete this document."));

        mockMvc.perform(delete("/documents/{id}", documentId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
    }

    private AuthResponse register(String name, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setName(name);
        request.setEmail(email);
        request.setPassword("secret123");
        return authService.register(request);
    }

    private int createDocument(AuthResponse user, String title, String content) throws Exception {
        String response = mockMvc.perform(post("/documents")
                        .header("Authorization", bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(documentRequest(title, content))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asInt();
    }

    private void shareDocument(
            AuthResponse owner,
            int documentId,
            String collaboratorEmail,
            DocumentRole role) throws Exception {
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setEmail(collaboratorEmail);
        request.setRole(role);

        mockMvc.perform(post("/documents/{id}/share", documentId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private DocumentRequest documentRequest(String title, String content) {
        DocumentRequest request = new DocumentRequest();
        request.setTitle(title);
        request.setContent(content);
        return request;
    }

    private String bearer(AuthResponse response) {
        return "Bearer " + response.getToken();
    }
}
