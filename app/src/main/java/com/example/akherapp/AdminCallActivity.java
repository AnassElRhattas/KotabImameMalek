package com.example.akherapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.akherapp.adapters.VideoCallAdapter;
import com.example.akherapp.adapters.UserMessageAdapter;
import com.example.akherapp.models.VideoCall;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

public class AdminCallActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "video_calls";
    private FirebaseFirestore db;
    private RecyclerView callsRecyclerView;
    private RecyclerView usersRecyclerView;
    private List<VideoCall> pendingCalls = new ArrayList<>();
    private List<String> userIdsWithMessages = new ArrayList<>();
    private VideoCallAdapter adapter;
    private UserMessageAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_call);

        createNotificationChannel();
        
        db = FirebaseFirestore.getInstance();
        callsRecyclerView = findViewById(R.id.callsRecyclerView);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        
        adapter = new VideoCallAdapter(pendingCalls, this::handleCallAction);
        callsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        callsRecyclerView.setAdapter(adapter);

        userAdapter = new UserMessageAdapter(userIdsWithMessages, this::openChat);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(userAdapter);

        listenForCalls();
        loadUsersWithMessages();
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
                    showCallNotification(call);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Video Calls",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for video call notifications");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void loadUsersWithMessages() {
        db.collection("chats")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Set<String> uniqueUserIds = new HashSet<>();
                for (QueryDocumentSnapshot chatDoc : queryDocumentSnapshots) {
                    String userId = chatDoc.getId();
                    chatDoc.getReference().collection("messages")
                        .get()
                        .addOnSuccessListener(messagesSnapshot -> {
                            for (QueryDocumentSnapshot messageDoc : messagesSnapshot) {
                                String senderId = messageDoc.getString("senderId");
                                if (senderId != null) {
                                    uniqueUserIds.add(senderId);
                                    userIdsWithMessages.clear();
                                    userIdsWithMessages.addAll(uniqueUserIds);
                                    userAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                }
            });
    }

    private void openChat(String userId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", userId);
        startActivity(intent);
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

    private void showCallNotification(VideoCall call) {
        Intent acceptIntent = new Intent(this, AdminCallActivity.class);
        acceptIntent.setAction("ACCEPT_CALL");
        acceptIntent.putExtra("callId", call.getId());
        PendingIntent acceptPendingIntent = PendingIntent.getActivity(this, 0, acceptIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent rejectIntent = new Intent(this, AdminCallActivity.class);
        rejectIntent.setAction("REJECT_CALL");
        rejectIntent.putExtra("callId", call.getId());
        PendingIntent rejectPendingIntent = PendingIntent.getActivity(this, 1, rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_videocam)
                .setContentTitle("مكالمة فيديو واردة")
                .setContentText("مكالمة جديدة من " + call.getCallerId())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_videocam, "قبول", acceptPendingIntent)
                .addAction(android.R.drawable.ic_delete, "رفض", rejectPendingIntent);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(call.getId().hashCode(), builder.build());
    }

    private void startJitsiMeeting(String roomId) {
        try {
            Toast.makeText(this, "جاري تحميل المكالمة...", Toast.LENGTH_SHORT).show();
            
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                .setServerURL(new URL("https://meet.jit.si"))
                .setRoom(roomId)
                .setAudioMuted(false)
                .setVideoMuted(false)
                .setFeatureFlag("welcomepage.enabled", false)
                .setFeatureFlag("prejoinpage.enabled", false)
                .setFeatureFlag("lobby-mode.enabled", false)
                .setFeatureFlag("meeting-password.enabled", false)
                .setFeatureFlag("security-options.enabled", false)
                .setFeatureFlag("invite.enabled", false)
                .build();

            JitsiMeetActivity.launch(this, options);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في بدء المكالمة", Toast.LENGTH_SHORT).show();
            
            // Update call status to failed
            db.collection("videoCalls").document(roomId.replace("chat_", ""))
                .update("status", "failed")
                .addOnFailureListener(err -> {
                    Log.e("AdminCallActivity", "Error updating call status", err);
                });
        }
    }
}