package com.example.la_buddy;

public class ChatMessage {
    private String text;
    private String senderId; // "admin" or the User's UID

    public ChatMessage() {} // Required for Firebase

    public ChatMessage(String text, String senderId) {
        this.text = text;
        this.senderId = senderId;
    }

    public String getText() { return text; }
    public String getSenderId() { return senderId; }
}
