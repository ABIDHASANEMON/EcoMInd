package com.memorymate.models;

import java.util.Date;

public class CloudReminder {
    private String id;
    private String type;
    private String title;
    private String description;
    private long timestamp;
    private boolean isRecurring;
    private int recurrenceType;
    private boolean vibrationEnabled;
    private boolean voiceEnabled;
    private boolean isActive;
    private String userId;
    private Date createdAt;
    private Date updatedAt;

    public CloudReminder() {
        // Required for Firestore
    }

    public CloudReminder(Reminder reminder, String userId) {
        this.id = String.valueOf(reminder.getId());
        this.type = reminder.getType();
        this.title = reminder.getTitle();
        this.description = reminder.getDescription();
        this.timestamp = reminder.getTimestamp();
        this.isRecurring = reminder.isRecurring();
        this.recurrenceType = reminder.getRecurrenceType();
        this.vibrationEnabled = reminder.isVibrationEnabled();
        this.voiceEnabled = reminder.isVoiceEnabled();
        this.isActive = reminder.isActive();
        this.userId = userId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }
    public int getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(int recurrenceType) { this.recurrenceType = recurrenceType; }
    public boolean isVibrationEnabled() { return vibrationEnabled; }
    public void setVibrationEnabled(boolean vibrationEnabled) { this.vibrationEnabled = vibrationEnabled; }
    public boolean isVoiceEnabled() { return voiceEnabled; }
    public void setVoiceEnabled(boolean voiceEnabled) { this.voiceEnabled = voiceEnabled; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}