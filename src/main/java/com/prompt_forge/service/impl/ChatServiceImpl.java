package com.prompt_forge.service.impl;

import com.prompt_forge.dto.chat.ChatResponse;
import com.prompt_forge.entity.ChatMessage;
import com.prompt_forge.entity.ChatSession;
import com.prompt_forge.entity.ChatSessionId;
import com.prompt_forge.mapper.ChatMapper;
import com.prompt_forge.reposityory.ChatMessageRepository;
import com.prompt_forge.reposityory.ChatSessionRepository;
import com.prompt_forge.security.AuthUtil;
import com.prompt_forge.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    private final AuthUtil authUtil;

    private final ChatMapper chatMapper;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = chatSessionRepository.getReferenceById(
                new ChatSessionId(projectId, userId)
        );

        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatChatSession(chatSession);

        return chatMapper.fromListOfChatMessage(chatMessageList);
    }
}
