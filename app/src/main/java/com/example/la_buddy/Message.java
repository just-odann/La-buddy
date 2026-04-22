package com.example.la_buddy;

public class Message {
    private String text;
    private String senderId; // The UID of the person who sent it
    private long timestamp;

    public Message() {} // Required for Firebase

    public Message(String text, String senderId, long timestamp) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = timestamp;
    }

    public String getText() { return text; }
    public String getSenderId() { return senderId; }
    public long getTimestamp() { return timestamp; }
}
