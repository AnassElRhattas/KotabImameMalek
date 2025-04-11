package com.example.akherapp.utils;

import android.app.Activity;
import android.app.Dialog;
import android.os.Build;
import android.util.Log;
import android.view.Window;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.adapters.NotificationAdapter;
import com.example.akherapp.models.NotificationItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    private static final String NOTIFICATIONS_COLLECTION = "notifications";
    private static final String USERS_COLLECTION = "users";
    
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static void showNotificationsDialog(Activity activity, String userId) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.notifications_dialog);

        RecyclerView recyclerView = dialog.findViewById(R.id.notificationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));
        
        NotificationAdapter adapter = new NotificationAdapter(activity, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Charger les notifications depuis Firestore
        FirebaseFirestore.getInstance()
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<NotificationItem> notifications = new ArrayList<>();
                queryDocumentSnapshots.forEach(document -> {
                    NotificationItem notification = document.toObject(NotificationItem.class);
                    notifications.add(notification);
                    Log.d("NotifDebug", "Documents trouvés: " + queryDocumentSnapshots.size());
                });
                adapter.updateNotifications(notifications);
            });

        dialog.show();
    }

    public static void sendNotificationToAllAdmins(String title, String body) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Récupérer tous les utilisateurs admin
        db.collection(USERS_COLLECTION)
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                    // Récupérer la liste des tokens FCM pour chaque admin
                    List<String> fcmTokens = (List<String>) document.get("fcmTokens");
                    if (fcmTokens != null && !fcmTokens.isEmpty()) {
                        // Envoyer la notification à chaque token
                        for (String token : fcmTokens) {
                            sendNotification(token, title, body);
                        }
                    }
                    
                    // Sauvegarder la notification dans Firestore
                    saveNotification(document.getId(), title, body);
                }
            });
    }

    private static void sendNotification(String token, String title, String body) {
        Map<String, String> data = new HashMap<>();
        data.put("title", title);
        data.put("body", body);

        FirebaseMessaging.getInstance().send(new RemoteMessage.Builder(token)
            .setData(data)
            .build());
    }

    public static void saveNotification(String userId, String title, String body) {
        NotificationItem notification = new NotificationItem(title, body, new Date());
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("userId", userId);
        notificationData.put("title", title);
        notificationData.put("message", body);
        notificationData.put("timestamp", new Date());

        FirebaseFirestore.getInstance()
            .collection(NOTIFICATIONS_COLLECTION)
            .add(notificationData);
    }
}
