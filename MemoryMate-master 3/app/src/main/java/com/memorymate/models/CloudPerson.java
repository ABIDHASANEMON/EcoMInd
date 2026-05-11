package com.memorymate.models;

import java.util.Date;

public class CloudPerson {
    private String id;
    private String name;
    private String photoUrl;
    private String relationship;
    private String phoneNumber;
    private String voiceNoteUrl;
    private String userId;
    private Date createdAt;
    private Date updatedAt;

    public CloudPerson() {
        // Required for Firestore
    }

    public CloudPerson(Person person, String userId) {
        this.id = String.valueOf(person.getId());
        this.name = person.getName();
        this.photoUrl = person.getPhotoPath();
        this.relationship = person.getRelationship();
        this.phoneNumber = person.getPhoneNumber();
        this.voiceNoteUrl = person.getVoiceNotePath();
        this.userId = userId;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getVoiceNoteUrl() { return voiceNoteUrl; }
    public void setVoiceNoteUrl(String voiceNoteUrl) { this.voiceNoteUrl = voiceNoteUrl; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}