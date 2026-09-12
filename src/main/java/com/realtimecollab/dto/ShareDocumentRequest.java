package com.realtimecollab.dto;

import com.realtimecollab.entity.DocumentRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ShareDocumentRequest {
    @Email(message = "Email must be valid")
    @NotBlank(message = "Collaborator email is required")
    private String email;
    private DocumentRole role;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public DocumentRole getRole() {
        return role;
    }

    public void setRole(DocumentRole role) {
        this.role = role;
    }
}
