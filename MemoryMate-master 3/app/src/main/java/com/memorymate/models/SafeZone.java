package com.memorymate.models;

public class SafeZone {
    private int id;
    private String name;
    private double latitude;
    private double longitude;
    private float radius;
    private boolean isActive;
    private String userId;

    public SafeZone() {}

    public SafeZone(String name, double latitude, double longitude, float radius, String userId) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.isActive = true;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}