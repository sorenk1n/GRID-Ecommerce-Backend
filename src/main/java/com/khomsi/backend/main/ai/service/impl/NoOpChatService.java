package com.khomsi.backend.main.ai.service.impl;

import com.khomsi.backend.main.ai.model.dto.ChatResponse;
import com.khomsi.backend.main.ai.service.ChatService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@ConditionalOnProperty(name = "app.ai.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpChatService implements ChatService {
    @Override
    public ChatResponse sendRequestToAiChat(String text) throws IOException {
        return new ChatResponse("AI 功能已临时关闭");
    }
}
