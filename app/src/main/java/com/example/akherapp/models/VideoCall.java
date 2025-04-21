package com.example.akherapp.models;

import com.google.firebase.Timestamp;

public class VideoCall {
    private String id;
    private String callerId;
    private String receiverId;
    private String status;
    private String roomId;
    private Timestamp timestamp;

    public VideoCall() {} // Required for Firestore

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getCallerId() { return callerId; }
    public void setCallerId(String callerId) { this.callerId = callerId; }
    
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}