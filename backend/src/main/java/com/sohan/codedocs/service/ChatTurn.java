package com.sohan.codedocs.service;

/**
 * One prior turn of conversation history, in Gemini's own role vocabulary
 * ("user"/"model") so GeminiChatClient can drop these straight into the
 * request's contents array with no translation.
 */
public record ChatTurn(String role, String text) {
    public static ChatTurn user(String text) {
        return new ChatTurn("user", text);
    }

    public static ChatTurn model(String text) {
        return new ChatTurn("model", text);
    }
}
