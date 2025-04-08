package com.example.akherapp;
public class Achievement {
    private String title;
    private int badgeResourceId;
    private float progress;  // 0 to 1
    private float requiredProgress;

    public Achievement(String title, int badgeResourceId, float progress, float requiredProgress) {
        this.title = title;
        this.badgeResourceId = badgeResourceId;
        this.progress = progress;
        this.requiredProgress = requiredProgress;
    }

    public String getTitle() { return title; }
    public int getBadgeResourceId() { return badgeResourceId; }
    public float getProgress() { return progress; }
    public float getRequiredProgress() { return requiredProgress; }
    public float getProgressPercentage() {
        return Math.min(progress / requiredProgress, 1f);
    }
}