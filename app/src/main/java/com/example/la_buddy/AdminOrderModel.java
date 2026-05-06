package com.example.la_buddy;

public class AdminOrderModel {
    private String uid, name, items, price, weight, status, historyId, category, addons, method;
    private int unreadCount;
    private long timestamp;
    private double latitude, longitude; // For GPS Navigation

    public AdminOrderModel() {}

    // Constructor updated to include ALL fields
    public AdminOrderModel(String uid, String name, String items, String price, String weight, String status,
                           String historyId, int unreadCount, long timestamp, String category, String addons,
                           String method, double latitude, double longitude) {
        this.uid = uid;
        this.name = name;
        this.items = items;
        this.price = price;
        this.weight = weight;
        this.status = status;
        this.historyId = historyId;
        this.unreadCount = unreadCount;
        this.timestamp = timestamp;
        this.category = category;
        this.addons = addons;
        this.method = method;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // --- GETTERS (Fixes the "Cannot resolve method" errors) ---
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getItems() { return items; }
    public String getPrice() { return price; }
    public String getWeight() { return weight; }
    public String getStatus() { return status; }
    public String getHistoryId() { return historyId; }
    public String getCategory() { return category; }
    public String getAddons() { return addons; }
    public String getMethod() { return method; }
    public int getUnreadCount() { return unreadCount; }
    public long getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    // --- SETTERS ---
    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setPrice(String price) { this.price = price; }
    public void setWeight(String weight) { this.weight = weight; }
    public void setStatus(String status) { this.status = status; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}