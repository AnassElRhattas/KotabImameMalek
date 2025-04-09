package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.UserComplaintsAdapter;
import com.example.akherapp.models.Complaint;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmitComplaintActivity extends BaseUserActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextInputEditText subjectInput;
    private TextInputEditText descriptionInput;
    private MaterialButton submitButton;
    private RecyclerView complaintsRecyclerView;
    private TextView emptyComplaintsText;
    private View progressBar;
    private FirebaseFirestore db;
    private UserComplaintsAdapter adapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_complaint);

        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();

        // Récupérer l'ID de l'utilisateur
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("id", null);

        initializeViews();
        setupToolbarAndNavigation();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_submit_complaint);

        setupRecyclerView();
        setupSubmitButton();
        loadUserComplaints();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        subjectInput = findViewById(R.id.subjectInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        submitButton = findViewById(R.id.submitButton);
        complaintsRecyclerView = findViewById(R.id.complaintsRecyclerView);
        emptyComplaintsText = findViewById(R.id.emptyComplaintsText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbarAndNavigation() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Load user info in navigation header
        View headerView = navigationView.getHeaderView(0);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                TextView nameView = headerView.findViewById(R.id.nav_header_name);
                                TextView roleView = headerView.findViewById(R.id.nav_header_role);
                                ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

                                String fullName = user.getFirstName() + " " + user.getLastName();
                                nameView.setText(fullName.trim());
                                roleView.setText("مستخدم");

                                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                    Glide.with(this)
                                            .load(user.getProfileImageUrl())
                                            .placeholder(R.drawable.default_profile_image)
                                            .error(R.drawable.default_profile_image)
                                            .circleCrop()
                                            .into(profileImageView);
                                } else {
                                    profileImageView.setImageResource(R.drawable.default_profile_image);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "خطأ في تحميل معلومات المستخدم", Toast.LENGTH_SHORT).show();
                    });
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
                finish();
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ViewScheduleActivity.class));
                finish();
            } else if (id == R.id.menu_complaints) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_voice_recognition) {
                startActivity(new Intent(this, VoiceRecognitionActivity.class));
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
            } else if (id == R.id.menu_logout) {
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Highlight active menu item
        navigationView.setCheckedItem(R.id.menu_complaints);
    }

    private void setupRecyclerView() {
        adapter = new UserComplaintsAdapter(this, new ArrayList<>());
        complaintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        complaintsRecyclerView.setAdapter(adapter);
    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> {
            String subject = subjectInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();

            if (subject.isEmpty()) {
                subjectInput.setError("يرجى إدخال الموضوع");
                return;
            }

            if (description.isEmpty()) {
                descriptionInput.setError("يرجى إدخال وصف المشكلة");
                return;
            }

            submitComplaint(subject, description);
        });
    }

    private void submitComplaint(String subject, String description) {
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        submitButton.setEnabled(false);

        // D'abord, récupérer les informations de l'utilisateur
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            String userFullName = user.getFirstName() + " " + user.getLastName();

                            Map<String, Object> complaint = new HashMap<>();
                            complaint.put("userId", userId);
                            complaint.put("userFullName", userFullName);
                            complaint.put("subject", subject);
                            complaint.put("description", description);
                            complaint.put("status", "pending");
                            complaint.put("createdAt", Timestamp.now());

                            db.collection("complaints")
                                    .add(complaint)
                                    .addOnSuccessListener(documentReference -> {
                                        Toast.makeText(this, "تم إرسال الشكوى بنجاح", Toast.LENGTH_SHORT).show();
                                        subjectInput.setText("");
                                        descriptionInput.setText("");
                                        loadUserComplaints();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "فشل في إرسال الشكوى", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnCompleteListener(task -> {
                                        progressBar.setVisibility(View.GONE);
                                        submitButton.setEnabled(true);
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل معلومات المستخدم", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    submitButton.setEnabled(true);
                });
    }

    private void loadUserComplaints() {
        if (userId == null) {
            Toast.makeText(this, "Erreur: ID utilisateur non trouvé", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("complaints")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Complaint> complaints = new ArrayList<>();
                    try {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Complaint complaint = document.toObject(Complaint.class);
                            if (complaint != null) {
                                complaint.setId(document.getId());
                                complaints.add(complaint);
                            }
                        }

                        adapter.updateComplaints(complaints);
                        updateEmptyView(complaints.isEmpty());

                        // Ajouter un log pour voir le nombre de plaintes chargées
                        System.out.println("Nombre de plaintes chargées : " + complaints.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur lors de la conversion des données: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace(); // Pour voir l'erreur détaillée dans les logs
                    Toast.makeText(this, "Erreur de chargement: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyComplaintsText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        complaintsRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    protected void checkLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (TextUtils.isEmpty(phone)) {
            return;
        }

        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean foundLinkedAccounts = false;
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        if (!doc.getId().equals(currentUserId)) {
                            foundLinkedAccounts = true;
                            break;
                        }
                    }

                    prefs.edit().putBoolean("has_linked_accounts", foundLinkedAccounts).apply();
                    invalidateOptionsMenu(); // Mettre à jour le menu
                });
    }
    @Override
    protected void showLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "خطأ في تحميل الحسابات المرتبطة", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    View headerView = navigationView.getHeaderView(0);
                    TextView linkedAccountsTitle = headerView.findViewById(R.id.linkedAccountsTitle);
                    LinearLayout linkedAccountsContainer = headerView.findViewById(R.id.linkedAccountsContainer);

                    linkedAccountsTitle.setVisibility(View.VISIBLE);
                    linkedAccountsContainer.removeAllViews();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        String userId = document.getId();

                        // Skip current account
                        if (!userId.equals(currentUserId)) {
                            View accountView = getLayoutInflater().inflate(R.layout.layout_linked_account, linkedAccountsContainer, false);

                            TextView nameText = accountView.findViewById(R.id.linkedAccountName);
                            MaterialButton switchButton = accountView.findViewById(R.id.btnSwitchAccount);
                            ShapeableImageView profileImage = accountView.findViewById(R.id.linkedAccountImage);

                            nameText.setText(user.getFirstName() + " " + user.getLastName());

                            // Load profile image
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.default_profile_image)
                                        .error(R.drawable.default_profile_image)
                                        .circleCrop()
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.default_profile_image);
                            }

                            switchButton.setOnClickListener(v -> switchToAccount(userId));
                            linkedAccountsContainer.addView(accountView);
                        }
                    }

                    drawerLayout.openDrawer(GravityCompat.START);
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading linked accounts", e);
                    Toast.makeText(this, "خطأ في تحميل الحسابات المرتبطة", Toast.LENGTH_SHORT).show();
                });
    }

    protected void switchToAccount(String userId) {
        // Sauvegarder le nouvel ID utilisateur
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putString("id", userId).apply();

        // Redémarrer l'activité
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}