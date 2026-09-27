package com.prompt_forge.llm.advisors;

import com.prompt_forge.dto.project.FileNode;
import com.prompt_forge.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileTreeContextAdvisor implements StreamAdvisor {

    private final ProjectFileService projectFileService;

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Map<String, Object> context = chatClientRequest.context();
        Long projectId = Long.parseLong(context.getOrDefault("projectId", 0).toString());

        ChatClientRequest augmentedChatClientRequest = augmentRequestWithFileTree(chatClientRequest, projectId);

        return streamAdvisorChain.nextStream(augmentedChatClientRequest);

    }

    private ChatClientRequest augmentRequestWithFileTree(ChatClientRequest request, Long projectId) {

        List<Message> incomingMessage = request.prompt().getInstructions();
        Message systemMessage = incomingMessage.stream()
                .filter(m -> m.getMessageType() == MessageType.SYSTEM)
                .findFirst()
                .orElse(null);

        List<Message> userMessage = incomingMessage.stream()
                .filter(m -> m.getMessageType() != MessageType.SYSTEM)
                .toList();

        List<Message> allMessage = new ArrayList<>();

        if (systemMessage != null) {
            allMessage.add(systemMessage);
        }

        List<FileNode> fileTree = projectFileService.getFileTree(projectId);
        String fileTreeContext = "\n\n ----- FILE_TREE ----- \n" + fileTree.toString() + "\n ----- FILE_TREE ----- \n";
        allMessage.add(new SystemMessage(fileTreeContext));

        allMessage.addAll(userMessage);

        return request
                .mutate()
                .prompt(new Prompt(allMessage, request.prompt().getOptions()))
                .build();
    }

    @Override
    public String getName() {
        return "FileTreeContextAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
