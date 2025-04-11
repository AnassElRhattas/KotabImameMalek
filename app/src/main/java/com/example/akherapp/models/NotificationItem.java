package com.example.akherapp.models;

import java.util.Date;

public class NotificationItem {
    private String title;
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

    public String getBody() {
        return body;
    }

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
