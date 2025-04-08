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
    private List<String> videoUrls;  // Changed to support multiple videos
    private Object timestamp;
    private String mediaType; // "image", "video", or "mixed"

    public News() {
        imageUrls = new ArrayList<>();
        videoUrls = new ArrayList<>();
    }

    public News(String title, String description, List<String> imageUrls, List<String> videoUrls) {
        this.title = title;
        this.description = description;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        this.videoUrls = videoUrls != null ? videoUrls : new ArrayList<>();
        this.timestamp = Timestamp.now();
        this.setMediaType();
    }

    private void setMediaType() {
        if (!imageUrls.isEmpty() && !videoUrls.isEmpty()) {
            this.mediaType = "mixed";
        } else if (!videoUrls.isEmpty()) {
            this.mediaType = "video";
        } else {
            this.mediaType = "image";
        }
    }

    // Add getters and setters for new fields
    public List<String> getVideoUrls() { return videoUrls; }
    public void setVideoUrls(List<String> videoUrls) {
        this.videoUrls = videoUrls != null ? videoUrls : new ArrayList<>();
        setMediaType();
    }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

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

    // Remove these methods as we're using videoUrls List instead
    /*
    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    */
}