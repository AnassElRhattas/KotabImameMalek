package com.example.akherapp.models;

import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class NotificationItem {
    private String title;
    @PropertyName("message")
    private String body;
    private Date timestamp;


    // Default constructor for Firestore
    public NotificationItem() {}

    public NotificationItem(String title, String body, Date timestamp) {
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    @PropertyName("message")
    public String getBody() {
        return body;
    }

    @PropertyName("message")
    public void setBody(String body) {
        this.body = body;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
