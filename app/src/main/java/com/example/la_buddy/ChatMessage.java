package com.example.la_buddy;

public class ChatMessage {
    private String text;
    private String sender; // 🚨 MUST be "sender", not "senderId"!
    private long timestamp;

    public ChatMessage() {} // Required for Firebase

    public ChatMessage(String text, String sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
}