package com.prompt_forge.controller;

import com.prompt_forge.dto.chat.ChatRequest;
import com.prompt_forge.dto.chat.ChatResponse;
import com.prompt_forge.service.AiGenerationService;
import com.prompt_forge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final AiGenerationService aiGenerationService;

    private final ChatService chatService;

    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest chatRequest) {
        return aiGenerationService.streamResponse(chatRequest.message(), chatRequest.projectId())
                .map(data -> ServerSentEvent.<String>builder()
                        .data(data)
                        .build());
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable Long projectId) {
        List<ChatResponse> chatHistory = chatService.getProjectChatHistory(projectId);
        return ResponseEntity.ok(chatHistory);

    }
}
