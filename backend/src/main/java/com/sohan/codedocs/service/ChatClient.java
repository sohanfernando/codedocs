package com.sohan.codedocs.service;

import java.util.List;
import java.util.function.Consumer;

public interface ChatClient {
    /** history is prior turns, oldest first — empty for a thread's first message. */
    String complete(String systemPrompt, List<ChatTurn> history, String userPrompt);

    /** Invokes onToken once per incremental chunk of generated text, in order. */
    void completeStream(String systemPrompt, List<ChatTurn> history, String userPrompt, Consumer<String> onToken);
}
