package com.prompt_forge.dto.chat;

public record ChatRequest(
        String message,
        Long projectId) {
}
