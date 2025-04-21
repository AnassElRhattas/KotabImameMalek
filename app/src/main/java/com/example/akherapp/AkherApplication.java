package com.example.akherapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

import org.jitsi.meet.sdk.JitsiMeet;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.net.URL;

public class AkherApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        try {
            JitsiMeetConferenceOptions defaultOptions
                    = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .build();

            JitsiMeet.setDefaultConferenceOptions(defaultOptions);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}