package com.example.akherapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.akherapp.adapters.VideoCallAdapter;
import com.example.akherapp.models.VideoCall;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

public class AdminCallActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView callsRecyclerView;
    private List<VideoCall> pendingCalls = new ArrayList<>();
    private VideoCallAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_call);

        db = FirebaseFirestore.getInstance();
        callsRecyclerView = findViewById(R.id.callsRecyclerView);
        
        adapter = new VideoCallAdapter(pendingCalls, this::handleCallAction);
        callsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        callsRecyclerView.setAdapter(adapter);

        listenForCalls();
    }

    private void listenForCalls() {
        db.collection("videoCalls")
            .whereEqualTo("status", "pending")
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) {
                    Toast.makeText(this, "خطأ في تحميل المكالمات", Toast.LENGTH_SHORT).show();
                    return;
                }

                pendingCalls.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    VideoCall call = doc.toObject(VideoCall.class);
                    call.setId(doc.getId());
                    pendingCalls.add(call);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private void handleCallAction(VideoCall call, boolean accept) {
        if (accept) {
            acceptCall(call);
        } else {
            rejectCall(call);
        }
    }

    private void acceptCall(VideoCall call) {
        db.collection("videoCalls").document(call.getId())
            .update("status", "accepted")
            .addOnSuccessListener(aVoid -> {
                startJitsiMeeting(call.getRoomId());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في قبول المكالمة", Toast.LENGTH_SHORT).show();
            });
    }

    private void rejectCall(VideoCall call) {
        db.collection("videoCalls").document(call.getId())
            .update("status", "rejected")
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في رفض المكالمة", Toast.LENGTH_SHORT).show();
            });
    }

    private void startJitsiMeeting(String roomId) {
        try {
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(new URL("https://meet.jit.si"))
                .setRoom(roomId)
                .setAudioMuted(false)
                .setVideoMuted(false)
                .build();

            JitsiMeetActivity.launch(this, options);
        } catch (Exception e) {
            Toast.makeText(this, "خطأ في بدء المكالمة", Toast.LENGTH_SHORT).show();
        }
    }
}