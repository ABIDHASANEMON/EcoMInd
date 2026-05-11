package com.memorymate.models;

public class Person {
    private int id;
    private String name;
    private String photoPath;
    private String relationship;
    private String phoneNumber;
    private String voiceNotePath;

    public Person() {
    }

    public Person(String name, String photoPath, String relationship, String phoneNumber, String voiceNotePath) {
        this.name = name;
        this.photoPath = photoPath;
        this.relationship = relationship;
        this.phoneNumber = phoneNumber;
        this.voiceNotePath = voiceNotePath;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getVoiceNotePath() {
        return voiceNotePath;
    }

    public void setVoiceNotePath(String voiceNotePath) {
        this.voiceNotePath = voiceNotePath;
    }
}