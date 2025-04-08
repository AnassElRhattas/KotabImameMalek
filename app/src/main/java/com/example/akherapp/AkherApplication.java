package com.example.akherapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class AkherApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
} 