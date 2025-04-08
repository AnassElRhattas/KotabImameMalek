package com.example.akherapp;
import com.example.akherapp.User;

public class UserRanking {
    private User user;
    private float totalProgress;

    public UserRanking(User user, float totalProgress) {
        this.user = user;
        this.totalProgress = totalProgress;
    }

    public User getUser() { return user; }
    public float getTotalProgress() { return totalProgress; }
}