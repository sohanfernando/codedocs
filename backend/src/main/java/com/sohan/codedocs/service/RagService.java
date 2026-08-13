package com.sohan.codedocs.service;

import com.sohan.codedocs.dto.request.ChatRequest;
import com.sohan.codedocs.dto.response.ChatResponse;

import java.util.UUID;

public interface RagService {
    ChatResponse answer(ChatRequest request, UUID ownerId);

    void answerStream(ChatRequest request, UUID ownerId, ChatStreamListener listener);
}
