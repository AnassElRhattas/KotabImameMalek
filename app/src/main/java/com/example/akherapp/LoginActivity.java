package com.example.akherapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText firstNameInput, lastNameInput, passwordInput;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Vérifier si l'utilisateur est déjà connecté
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("isAdmin", false);
        String userId = prefs.getString("id", null);
        if (userId != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.putExtra("isAdmin", isAdmin);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return;
        }
        
        setContentView(R.layout.activity_login);
        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        passwordInput = findViewById(R.id.passwordInput);
    }

    private void setupListeners() {
        findViewById(R.id.loginButton).setOnClickListener(v -> handleLogin());
        findViewById(R.id.registerButton).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void handleLogin() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال جميع البيانات", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري تسجيل الدخول...");
        progressDialog.show();

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                    return;
                }

                String fcmToken = task.getResult();

                db.collection("users")
                    .whereEqualTo("firstName", firstName)
                    .whereEqualTo("lastName", lastName)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                            // Ajouter le token FCM à la liste des tokens de l'utilisateur
                            db.collection("users").document(document.getId())
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    java.util.List<String> fcmTokens = (java.util.List<String>) userSnapshot.get("fcmTokens");
                                    if (fcmTokens == null) {
                                        fcmTokens = new java.util.ArrayList<>();
                                    }
                                    if (!fcmTokens.contains(fcmToken)) {
                                        fcmTokens.add(fcmToken);
                                        db.collection("users").document(document.getId())
                                            .update("fcmTokens", fcmTokens)
                                            .addOnSuccessListener(aVoid -> handleLoginSuccess(document))
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(LoginActivity.this,
                                                    "خطأ في تحديث التوكن",
                                                    Toast.LENGTH_SHORT).show();
                                            });
                                    } else {
                                        handleLoginSuccess(document);
                                    }
                                });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(LoginActivity.this,
                                "الاسم أو كلمة المرور غير صحيحة",
                                Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this,
                            "خطأ في تسجيل الدخول",
                            Toast.LENGTH_SHORT).show();
                    });
            });
    }

    private void handleLoginSuccess(DocumentSnapshot userDoc) {
        User user = userDoc.toObject(User.class);
        if (user != null) {
            // Sauvegarder les informations de l'utilisateur
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("id", userDoc.getId());
            editor.putString("firstName", user.getFirstName());
            editor.putString("lastName", user.getLastName());
            editor.putString("phone", user.getPhone());
            editor.putString("role", user.getRole());
            editor.putString("profileImageUrl", user.getProfileImageUrl());
            editor.apply();

            // Vérifier les comptes liés avant de démarrer MainActivity
            checkLinkedAccounts(user.getPhone(), userDoc.getId());
        }
    }

    private void checkLinkedAccounts(String phone, String currentUserId) {
        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean hasLinkedAccounts = false;
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    if (!doc.getId().equals(currentUserId)) {
                        hasLinkedAccounts = true;
                        break;
                    }
                }

                // Sauvegarder l'information sur les comptes liés
                SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("has_linked_accounts", hasLinkedAccounts).apply();

                // Démarrer MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                // En cas d'erreur, continuer quand même vers MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
    }
}