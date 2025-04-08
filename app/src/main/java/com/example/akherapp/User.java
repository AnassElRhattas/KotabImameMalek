package com.example.akherapp;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String birthDate;
    private String password;
    private String profileImageUrl;
    private String teacher;
    private String role;
    private Float week1Progress;
    private Float week2Progress;
    private Float week3Progress;
    private Float week4Progress;
    private String fatherFirstName;
    private String fatherLastName;
    private Date registrationDate;
    private String certificatePath;
    private Map<String, Object> documents;
    private String certificateUrl;
    private boolean documentsSubmitted;
    private boolean documentsVerified;
    private boolean documentsRejected;
    private boolean registrationCompletionNotified;
    private boolean registrationCompleted;
    private List<String> fcmTokens;

    public User() {
        // Required empty constructor for Firestore
        this.documentsSubmitted = false;
        this.documentsVerified = false;
        this.documentsRejected = false;
    }

    public User(String firstName, String lastName, String phone, String birthDate,
                String password, String profileImageUrl, String teacher) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.password = password;
        this.profileImageUrl = profileImageUrl;
        this.teacher = teacher;
        this.role = "student"; // Rôle par défaut
        this.week1Progress = 0f;
        this.week2Progress = 0f;
        this.week3Progress = 0f;
        this.week4Progress = 0f;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBirthDate() { return birthDate; }

    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public void setBirthDateFromAny(Object value) {
        if (value instanceof Long) {
            this.birthDate = String.valueOf(value);
        } else if (value instanceof String) {
            this.birthDate = (String) value;
        } else if (value == null) {
            this.birthDate = null;
        }
    }

    public Long getBirthDateAsLong() {
        if (birthDate == null || birthDate.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(birthDate);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @com.google.firebase.firestore.Exclude
    public void setBirthDateFromFirestore(Object value) {
        setBirthDateFromAny(value);
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Progress getters and setters
    public Float getWeek1Progress() { return week1Progress; }
    public void setWeek1Progress(Float progress) { this.week1Progress = progress; }

    public Float getWeek2Progress() { return week2Progress; }
    public void setWeek2Progress(Float progress) { this.week2Progress = progress; }

    public Float getWeek3Progress() { return week3Progress; }
    public void setWeek3Progress(Float progress) { this.week3Progress = progress; }

    public Float getWeek4Progress() { return week4Progress; }
    public void setWeek4Progress(Float progress) { this.week4Progress = progress; }
    public String getFatherFirstName() { return fatherFirstName; }
    public void setFatherFirstName(String fatherFirstName) { this.fatherFirstName = fatherFirstName; }

    public String getFatherLastName() { return fatherLastName; }
    public void setFatherLastName(String fatherLastName) { this.fatherLastName = fatherLastName; }

    public Date getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getCertificatePath() {
        return certificatePath;
    }

    public void setCertificatePath(String certificatePath) {
        this.certificatePath = certificatePath;
    }

    // Add these getter and setter methods
    public Map<String, Object> getDocuments() {
        return documents;
    }

    public void setDocuments(Map<String, Object> documents) {
        this.documents = documents;
    }

    public String getCertificateUrl() {
        return certificateUrl;
    }

    public void setCertificateUrl(String certificateUrl) {
        this.certificateUrl = certificateUrl;
    }

    public boolean isDocumentsSubmitted() {
        return documentsSubmitted;
    }

    public void setDocumentsSubmitted(boolean documentsSubmitted) {
        this.documentsSubmitted = documentsSubmitted;
    }

    public boolean isDocumentsVerified() {
        return documentsVerified;
    }

    public void setDocumentsVerified(boolean documentsVerified) {
        this.documentsVerified = documentsVerified;
    }

    public boolean isDocumentsRejected() {
        return documentsRejected;
    }

    public void setDocumentsRejected(boolean documentsRejected) {
        this.documentsRejected = documentsRejected;
    }

    public boolean isDocumentVerified(String documentType) {
        Map<String, Object> documents = getDocuments();
        if (documents != null) {
            Boolean verified = (Boolean) documents.get(documentType + "_verified");
            return verified != null && verified;
        }
        return false;
    }

    public boolean isDocumentRejected(String documentType) {
        Map<String, Object> documents = getDocuments();
        if (documents != null) {
            Boolean rejected = (Boolean) documents.get(documentType + "_rejected");
            return rejected != null && rejected;
        }
        return false;
    }

    public boolean isRegistrationCompletionNotified() {
        return registrationCompletionNotified;
    }

    public void setRegistrationCompletionNotified(boolean registrationCompletionNotified) {
        this.registrationCompletionNotified = registrationCompletionNotified;
    }

    public boolean isRegistrationCompleted() {
        return registrationCompleted;
    }

    public void setRegistrationCompleted(boolean registrationCompleted) {
        this.registrationCompleted = registrationCompleted;
    }

    public void setDocumentVerified(String documentType, boolean verified) {
        if (documents == null) {
            documents = new HashMap<>();
        }
        documents.put(documentType + "_verified", verified);
    }

    public void setDocumentRejected(String documentType, boolean rejected) {
        if (documents == null) {
            documents = new HashMap<>();
        }
        documents.put(documentType + "_rejected", rejected);
    }


    // Add these getter and setter methods
    public List<String> getFcmTokens() {
        return fcmTokens != null ? fcmTokens : new ArrayList<>();
    }

    public void setFcmTokens(List<String> fcmTokens) {
        this.fcmTokens = fcmTokens;
    }
}