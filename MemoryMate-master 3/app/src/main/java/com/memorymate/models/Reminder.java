package com.memorymate.models;

public class Reminder {
    private int id;
    private String type; // "medicine", "meal", "appointment"
    private String title;
    private String description;
    private long timestamp;
    private boolean isRecurring;
    private int recurrenceType; // 0=daily, 1=weekly, 2=monthly
    private boolean vibrationEnabled;
    private boolean voiceEnabled;
    private boolean isActive;

    public Reminder() {
    }

    public Reminder(String type, String title, String description, long timestamp,
                    boolean isRecurring, int recurrenceType, boolean vibrationEnabled,
                    boolean voiceEnabled) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.isRecurring = isRecurring;
        this.recurrenceType = recurrenceType;
        this.vibrationEnabled = vibrationEnabled;
        this.voiceEnabled = voiceEnabled;
        this.isActive = true;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRecurring() {
        return isRecurring;
    }

    public void setRecurring(boolean recurring) {
        isRecurring = recurring;
    }

    public int getRecurrenceType() {
        return recurrenceType;
    }

    public void setRecurrenceType(int recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void setVoiceEnabled(boolean voiceEnabled) {
        this.voiceEnabled = voiceEnabled;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}