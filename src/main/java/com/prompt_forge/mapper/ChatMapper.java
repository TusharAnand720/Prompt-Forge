package com.prompt_forge.mapper;

import com.prompt_forge.dto.chat.ChatResponse;
import com.prompt_forge.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMapper {

    List<ChatResponse> fromListOfChatMessage(List<ChatMessage> chatMessageList);
}
