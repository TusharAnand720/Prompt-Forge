package com.prompt_forge.service.impl;

import com.prompt_forge.entity.ChatEvent;
import com.prompt_forge.entity.ChatMessage;
import com.prompt_forge.entity.ChatSession;
import com.prompt_forge.entity.ChatSessionId;
import com.prompt_forge.entity.Project;
import com.prompt_forge.entity.User;
import com.prompt_forge.enums.ChatEventType;
import com.prompt_forge.enums.MessageRole;
import com.prompt_forge.error.ResourceNotFoundException;
import com.prompt_forge.llm.LLMResponseParser;
import com.prompt_forge.llm.PromptUtils;
import com.prompt_forge.llm.advisors.FileTreeContextAdvisor;
import com.prompt_forge.llm.tools.CodeGenerationTools;
import com.prompt_forge.reposityory.ChatEventRepository;
import com.prompt_forge.reposityory.ChatMessageRepository;
import com.prompt_forge.reposityory.ChatSessionRepository;
import com.prompt_forge.reposityory.ProjectRepository;
import com.prompt_forge.reposityory.UserRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.AiGenerationService;
import com.prompt_forge.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGenerationServiceImpl implements AiGenerationService {

    private final ChatClient chatClient;

    private final AuthUtil authUtil;

    private final ProjectFileService projectFileService;

    private final FileTreeContextAdvisor fileTreeContextAdvisor;

    private final LLMResponseParser llmResponseParser;

    private final ChatSessionRepository chatSessionRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;

    @Override
    public Flux<String> streamResponse(String userMessage, Long projectId) {
        try {
            Long userId = authUtil.getCurrentUserId();

            ChatSession chatSession = createChatSessionIfNotExists(projectId, userId);

            Map<String, Object> advisorParams = Map.of(
                    "userId", userId,
                    "projectId", projectId
            );

            StringBuilder fullResponseBuffer = new StringBuilder();

            CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectFileService, projectId);

            AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
            AtomicReference<Long> endTime = new AtomicReference<>(0L);

            return chatClient.prompt()
                    .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                    .user(userMessage)
                    .tools(codeGenerationTools)
                    .advisors(advisorSpec -> {
                        advisorSpec.params(advisorParams);
                        advisorSpec.advisors(fileTreeContextAdvisor);
                    })
                    .stream()
                    .chatResponse()
                    .doOnNext(response -> {
                        Generation g = response.getResult();
                        String content = g == null ? "" : Optional.ofNullable(g.getOutput().getText()).orElse("");
                        if (content != null && !content.isEmpty() && endTime.get() == 0L) {
                            endTime.set(System.currentTimeMillis());
                        }
                        fullResponseBuffer.append(content);
                    })
                    .doOnComplete(() -> {
                        CompletableFuture.runAsync(() -> {
                            long duration = (endTime.get() - startTime.get()) / 1000;
                            finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration);
                        });
                    })
                    .doOnError(error -> log.error("Error during streaming for project : {} , error : {}", projectId, error.getMessage()))
                    .map(response -> {
                        Generation g = response.getResult();
                        return g == null ? "" : Optional.ofNullable(g.getOutput().getText()).orElse("");
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ChatSession createChatSessionIfNotExists(Long projectId, Long userId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);
        if (chatSession == null) {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("project", projectId.toString()));

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("user", userId.toString()));

            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .project(project)
                    .user(user)
                    .build();

            chatSession = chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }

    private void finalizeChats(String userPrompt, ChatSession chatSession, String fullText, long duration) {

        // saving user message
        ChatMessage userChatMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.USER)
                .content(userPrompt)
                .build();
        chatMessageRepository.save(userChatMessage);

        // saving LLM message
        ChatMessage assistanceChatMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.ASSISTANT)
                .content(fullText)
                .build();

        assistanceChatMessage = chatMessageRepository.save(assistanceChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistanceChatMessage);

        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .chatMessage(assistanceChatMessage)
                .content("Thought for the response generated in " + duration + " seconds.")
                .sequenceOrder(0)
                .build());

        chatEventRepository.saveAll(chatEventList);

        chatEventList.stream()
                .filter(event -> event.getType() == ChatEventType.FILE_EDIT)
                .forEach(event ->
                        projectFileService.saveFile(chatSession.getId().getProjectId(), event.getFilePath(), event.getContent())
                );

        chatEventRepository.saveAll(chatEventList);
    }
}
