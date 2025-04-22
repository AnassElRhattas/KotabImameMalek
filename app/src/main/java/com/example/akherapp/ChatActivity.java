package com.example.akherapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.MessageAdapter;
import com.example.akherapp.models.Message;
import com.example.akherapp.utils.NotificationUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import java.net.URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String chatId, currentUserId, otherUserId;

    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton;


    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getString("id", null);
        otherUserId = getIntent().getStringExtra("otherUserId");

        String adminName = getIntent().getStringExtra("adminName");
        if (adminName != null) {
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(adminName);
        }

        chatId = getChatId(currentUserId, otherUserId);

        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);


        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        sendButton.setOnClickListener(v -> sendMessage());
        findViewById(R.id.videoCallButton).setOnClickListener(v -> startVideoCall());

        listenForMessages();
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        Log.d("ChatActivity", "currentUserId: " + currentUserId);
        Log.d("ChatActivity", "otherUserId: " + otherUserId);
    }

    private String getChatId(String user1, String user2) {
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("Both user IDs must not be null");

        }
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }


    private void sendMessage() {
        String text = messageInput.getText().toString();
        if (text.isEmpty()) return;

        Message message = new Message(currentUserId, text, Timestamp.now());
        db.collection("chats").document(chatId)
                .collection("messages").add(message);

        messageInput.setText("");
    }

    private void listenForMessages() {
        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    messageList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        messageList.add(doc.toObject(Message.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void startVideoCall() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    123);
            return;
        }

        new AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle("تأكيد المكالمة")
                .setMessage("هل تريد بدء مكالمة فيديو؟")
                .setIcon(R.drawable.ic_videocam)
                .setPositiveButton("نعم", (dialog, which) -> {
                    // Get user's full name and admin's FCM token
                    db.collection("users").document(currentUserId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);
                                    if (user != null) {
                                        String userFullName = user.getFirstName() + " " + user.getLastName();

                                        // Get admin's FCM token
                                        db.collection("users")
                                                .whereEqualTo("role", "admin")
                                                .get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                                                        List<String> adminFcmTokens = (List<String>) document.get("fcmTokens");

                                                        // Create call data
                                                        String callId = chatId + "_" + System.currentTimeMillis();
                                                        Map<String, Object> callData = new HashMap<>();
                                                        callData.put("callerId", currentUserId);
                                                        callData.put("receiverId", otherUserId);
                                                        callData.put("status", "pending");
                                                        callData.put("timestamp", Timestamp.now());
                                                        callData.put("roomId", "chat_" + callId);

                                                        db.collection("videoCalls")
                                                                .document(callId)
                                                                .set(callData)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    // Send notification to admin
                                                                    if (adminFcmTokens != null) {
                                                                        for (String token : adminFcmTokens) {
                                                                            NotificationUtils.sendNotificationToAdminCall(token, userFullName);
                                                                        }
                                                                    }

                                                                    // Create Firestore notification for admin
                                                                    Map<String, Object> notification = new HashMap<>();
                                                                    notification.put("title", "مكالمة واردة");
                                                                    notification.put("message", "تم استلام مكالمة جديدة من " + userFullName);
                                                                    notification.put("timestamp", new Date());
                                                                    notification.put("type", "new_call");
                                                                    notification.put("userId", "admin");
                                                                    notification.put("studentId", currentUserId);
                                                                    notification.put("callId", callId);

                                                                    db.collection("notifications")
                                                                            .add(notification)
                                                                            .addOnSuccessListener(notifRef -> {
                                                                                Log.d("ChatActivity", "Notification sent to admin");
                                                                                // Listen for call response
                                                                                listenForCallResponse(callId);
                                                                            })
                                                                            .addOnFailureListener(e -> {
                                                                                Log.e("ChatActivity", "Error sending notification", e);
                                                                            });
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Toast.makeText(this, "فشل في بدء المكالمة", Toast.LENGTH_SHORT).show();
                                                                });
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(this, "فشل في تحميل بيانات المشرف", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                }
                            });
                })
                .setNegativeButton("لا", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void listenForCallResponse(String callId) {
        db.collection("videoCalls")
                .document(callId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) return;

                    String status = snapshot.getString("status");
                    if ("accepted".equals(status)) {
                        // L'admin a accepté l'appel, démarrer Jitsi
                        String roomId = snapshot.getString("roomId");
                        startJitsiMeeting(roomId);
                    } else if ("rejected".equals(status)) {
                        Toast.makeText(this, "تم رفض المكالمة", Toast.LENGTH_SHORT).show();
                    }
                });
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
                    Log.e("ChatActivity", "Error updating call status", err);
                });
        }
    }

    private void showDevelopmentDialogRamadan() {
        new AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle("إعلام")
                .setMessage("التسجيل غير متاح حاليا حتى موسم رمضان")
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("موافق", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

}

