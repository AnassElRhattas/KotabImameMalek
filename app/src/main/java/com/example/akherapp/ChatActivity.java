package com.example.akherapp;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.akherapp.adapters.MessageAdapter;
import com.example.akherapp.models.Message;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import java.net.URL;

import java.util.ArrayList;
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
        otherUserId = "b450716b-cb70-4950-ae36-a39c028cbe2a";

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

        // Créer une notification d'appel dans Firestore
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
                    // Écouter la réponse de l'admin
                    listenForCallResponse(callId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في بدء المكالمة", Toast.LENGTH_SHORT).show();
                });
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
            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(new URL("https://meet.jit.si"))
                    .setRoom(roomId)
                    .setAudioMuted(false)
                    .setVideoMuted(false)
                    .build();

            JitsiMeetActivity.launch(this, options);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في بدء المكالمة", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

}

