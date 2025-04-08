package com.example.akherapp.models;

import java.util.Date;

public class ScheduleUpdate {
    private Date timestamp;
    private String scheduleUrl;

    // Empty constructor for Firestore
    public ScheduleUpdate() {}

    public ScheduleUpdate(Date timestamp, String scheduleUrl) {
        this.timestamp = timestamp;
        this.scheduleUrl = scheduleUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getScheduleUrl() {
        return scheduleUrl;
    }

    public void setScheduleUrl(String scheduleUrl) {
        this.scheduleUrl = scheduleUrl;
    }
}