package com.realtimecollab.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public class DocumentPatchRequest {

    @Size(max = 255, message = "Title must be at most 255 characters")
    private String title;

    private String content;

    @AssertTrue(message = "At least one field must be provided")
    public boolean hasAtLeastOneField() {
        return title != null || content != null;
    }

    @AssertTrue(message = "Title must not be blank when provided")
    public boolean isTitleValid() {
        return title == null || !title.isBlank();
    }

    @AssertTrue(message = "Content must not be blank when provided")
    public boolean isContentValid() {
        return content == null || !content.isBlank();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
