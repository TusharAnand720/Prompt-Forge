package com.prompt_forge.llm;

import com.prompt_forge.entity.ChatEvent;
import com.prompt_forge.entity.ChatMessage;
import com.prompt_forge.enums.ChatEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class LLMResponseParser {

    private static final Pattern GENERATE_TAG_PATTERN = Pattern.compile(
            "(<(message|file|tool)([^>]*)>)([\\s\\S]*?)(</\\2>)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile(
            "(path|args)=\"([^\"]+)\""
    );

    public List<ChatEvent> parseChatEvents(String fullResponse, ChatMessage parentChatMessage) {

        List<ChatEvent> chatEvents = new ArrayList<>();
        int orderCount = 1;
        Matcher matcher = GENERATE_TAG_PATTERN.matcher(fullResponse);

        while (matcher.find()) {
            String tagName = matcher.group(2).toLowerCase();
            String attributes = matcher.group(3);
            String content = matcher.group(4).trim();

            Map<String, String> attrMap = extractAttributes(attributes);

            ChatEvent.ChatEventBuilder builder = ChatEvent.builder()
                    .chatMessage(parentChatMessage)
                    .content(content)
                    .sequenceOrder(orderCount++);

            switch (tagName) {
                case "message" -> builder.type(ChatEventType.MESSAGE);
                case "file" -> {
                    builder.type(ChatEventType.FILE_EDIT);
                    builder.filePath(attrMap.get("path"));
                }
                case "tool" -> {
                    builder.type(ChatEventType.TOOL_LOG);
                    builder.metadata(attrMap.get("args"));
                }
                default -> {
                    log.warn("Unknown tag: {}", tagName);
                    continue;
                }
            }
            chatEvents.add(builder.build());
        }
        return chatEvents;
    }

    private Map<String, String> extractAttributes(String attributes) {
        Map<String, String> attributesMap = new HashMap<>();
        if (attributes == null) {
            return attributesMap;
        }
        Matcher matcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (matcher.find()) {
            attributesMap.put(matcher.group(1).toLowerCase(), matcher.group(2));
        }

        return attributesMap;
    }
}
