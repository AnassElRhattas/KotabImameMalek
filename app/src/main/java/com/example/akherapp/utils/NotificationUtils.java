package com.example.akherapp.utils;

import android.util.Log;

import com.example.akherapp.MyApplication;
import com.example.akherapp.R;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class NotificationUtils {
    private static final String TAG = "NotificationUtils";
    private static final String MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String[] SCOPES = { MESSAGING_SCOPE };
    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/hamid-9f53b/messages:send";

    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    public static void sendNewUserNotification(String userName) {
        Log.d(TAG, "Début de l'envoi de notification pour le nouvel utilisateur: " + userName);
        
        FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Nombre d'admins trouvés: " + queryDocumentSnapshots.size());
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    List<String> fcmTokens = (List<String>) document.get("fcmTokens");
                    if (fcmTokens != null && !fcmTokens.isEmpty()) {
                        Log.d(TAG, "Tokens FCM trouvés pour l'admin " + document.getId() + ": " + fcmTokens.size() + " tokens");
                        for (String token : fcmTokens) {
                            sendNotificationToAdmin(token, userName);
                        }
                    } else {
                        Log.w(TAG, "Aucun token FCM trouvé pour l'admin " + document.getId());
                    }
                }
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Erreur lors de la récupération des admins: " + e.getMessage()));
    }

    private static void sendNotificationToAdmin(String adminToken, String userName) {
        try {
            Log.d(TAG, "Préparation de la notification pour l'admin");
            JSONObject message = new JSONObject();
            JSONObject notification = new JSONObject();
            notification.put("title", "تسجيل طالب جديد");
            notification.put("body", "تم تسجيل طالب جديد: " + userName);

            message.put("message", new JSONObject()
                .put("token", adminToken)
                .put("notification", notification));

            Log.d(TAG, "Corps de la notification: " + message.toString());

            RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                message.toString());

            // Exécuter dans un thread en arrière-plan
            new Thread(() -> {
                try {
                    String accessToken = getAccessToken();
                    Request request = new Request.Builder()
                        .url(FCM_URL)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .build();

                    Log.d(TAG, "Envoi de la requête FCM à: " + FCM_URL);

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification envoyée avec succès. Réponse: " + responseBody);
                    } else {
                        Log.e(TAG, "Échec de l'envoi de la notification. Code: " + response.code() + ", Réponse: " + responseBody);
                    }
                    response.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erreur lors de l'envoi de la notification", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la notification", e);
        }
    }

    public static void sendNotificationToAllUsers(String title, String message) {
        Log.d(TAG, "Début de l'envoi de notification à tous les utilisateurs");
        
        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Nombre d'utilisateurs trouvés: " + queryDocumentSnapshots.size());
                
                for (DocumentSnapshot document : queryDocumentSnapshots) {
                    List<String> fcmTokens = (List<String>) document.get("fcmTokens");
                    if (fcmTokens != null && !fcmTokens.isEmpty()) {
                        for (String token : fcmTokens) {
                            sendNotification(token, title, message);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erreur lors de la récupération des utilisateurs", e);
            });
    }

    public static void sendNotification(String token, String title, String message) {
        try {
            Log.d(TAG, "Préparation de la notification");
            JSONObject notificationMessage = new JSONObject();
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);

            notificationMessage.put("message", new JSONObject()
                .put("token", token)
                .put("notification", notification));

            RequestBody requestBody = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                notificationMessage.toString());

            new Thread(() -> {
                try {
                    String accessToken = getAccessToken();
                    Request request = new Request.Builder()
                        .url(FCM_URL)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .build();

                    Response response = client.newCall(request).execute();
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification envoyée avec succès. Réponse: " + responseBody);
                    } else {
                        Log.e(TAG, "Échec de l'envoi de la notification. Code: " + response.code() + ", Réponse: " + responseBody);
                    }
                    response.close();
                } catch (IOException e) {
                    Log.e(TAG, "Erreur lors de l'envoi de la notification", e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la préparation de la notification", e);
        }
    }

    private static String getAccessToken() throws IOException {
        Log.d(TAG, "Récupération du token d'accès");
        try {
            InputStream serviceAccount = MyApplication.getAppContext().getResources()
                .openRawResource(R.raw.service_account);

            GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(serviceAccount)
                .createScoped(Arrays.asList(SCOPES));

            googleCredentials.refresh();
            String token = googleCredentials.getAccessToken().getTokenValue();
            Log.d(TAG, "Token d'accès récupéré avec succès");
            return token;
        } catch (IOException e) {
            Log.e(TAG, "Erreur lors de la récupération du token d'accès", e);
            throw e;
        }
    }

    public static void updateUserFcmToken(String userId, String token) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                List<String> fcmTokens = (List<String>) documentSnapshot.get("fcmTokens");
                if (fcmTokens == null) {
                    fcmTokens = new ArrayList<>();
                }
                if (!fcmTokens.contains(token)) {
                    fcmTokens.add(token);
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update("fcmTokens", fcmTokens)
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Error updating FCM tokens: " + e.getMessage()));
                }
            })
            .addOnFailureListener(e -> 
                Log.e(TAG, "Error getting user document: " + e.getMessage()));
    }
}
