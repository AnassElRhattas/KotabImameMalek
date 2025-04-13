package com.example.akherapp;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class News {
    private String id;
    private String title;
    private String description;
    private List<String> imageUrls;
    private String videoUrl;
    private Object timestamp;
    private List<String> likes; // Pour stocker les IDs des utilisateurs qui ont aimé

    public News() {
        imageUrls = new ArrayList<>();
        likes = new ArrayList<>(); // Initialiser la liste des likes
    }

    public News(String title, String description, List<String> imageUrls) {
        this.title = title;
        this.description = description;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.timestamp = Timestamp.now();
        this.likes = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { 
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>(); 
    }

    public Timestamp getTimestamp() {
        if (timestamp instanceof Timestamp) {
            return (Timestamp) timestamp;
        } else if (timestamp instanceof Long) {
            return new Timestamp(new Date((Long) timestamp));
        } else if (timestamp instanceof Date) {
            return new Timestamp((Date) timestamp);
        }
        return Timestamp.now(); // Default fallback
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public List<String> getLikes() {
        return likes != null ? likes : new ArrayList<>();
    }

    public void setLikes(List<String> likes) {
        this.likes = likes != null ? likes : new ArrayList<>();
    }

    public int getLikesCount() {
        return getLikes().size();
    }
}