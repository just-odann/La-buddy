package com.example.la_buddy;

public class InboxModel {
    private String uid;
    private String name;
    private String lastMessage;
    private String timestamp;
    private boolean isUnread;

    // 🚨 THE FIX: Add this numeric field for sorting 🚨
    private long sortTimestamp;

    public InboxModel() {}

    public InboxModel(String uid, String name, String lastMessage, String timestamp, boolean isUnread) {
        this.uid = uid;
        this.name = name;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.isUnread = isUnread;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isUnread() { return isUnread; }
    public void setUnread(boolean unread) { isUnread = unread; }

    // 🚨 THESE ARE THE METHODS YOUR ACTIVITY IS LOOKING FOR 🚨
    public long getSortTimestamp() { return sortTimestamp; }
    public void setSortTimestamp(long sortTimestamp) { this.sortTimestamp = sortTimestamp; }
}