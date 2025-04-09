package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.app.Dialog;
import android.view.Window;
import com.github.chrisbanes.photoview.PhotoView;

public class ViewScheduleActivity extends BaseUserActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView scheduleImageView;
    private TextView noScheduleText;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_schedule);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        loadSchedule();
        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_schedule);
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        scheduleImageView = findViewById(R.id.scheduleImageView);
//        noScheduleText = findViewById(R.id.noScheduleText);
        
        // Load last update time
        loadLastUpdateTime();
    }

    private void loadLastUpdateTime() {
        db.collection("schedule_stats").document("latest")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String lastUpdate = documentSnapshot.getString("lastUpdate");
                    if (lastUpdate != null) {
                        TextView lastUpdateText = findViewById(R.id.lastUpdateText);
                        lastUpdateText.setText("آخر تحديث: " + lastUpdate);
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ViewScheduleActivity", "Error loading last update time", e);
            });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
                finish();
            } else if (id == R.id.menu_voice_recognition) {
                startActivity(new Intent(this, VoiceRecognitionActivity.class));
            } else if (id == R.id.menu_schedule) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_submit_complaint) {
                startActivity(new Intent(this, SubmitComplaintActivity.class));
                finish();
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
            return true;
        });

        View headerView = navigationView.getHeaderView(0);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                TextView nameView = headerView.findViewById(R.id.nav_header_name);
                                TextView roleView = headerView.findViewById(R.id.nav_header_role);
                                ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

                                String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                        (user.getLastName() != null ? user.getLastName() : "");
                                nameView.setText(fullName.trim());
                                roleView.setText("طالب");

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
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "خطأ في تحميل معلومات المستخدم",
                            Toast.LENGTH_SHORT).show());
        }
    }

    private void loadSchedule() {
        StorageReference scheduleRef = storage.getReference().child("schedules").child("schedule_latest");
        scheduleRef.getMetadata().addOnSuccessListener(metadata -> {
            scheduleRef.getDownloadUrl().addOnSuccessListener(uri -> {
                showImage(uri);
            }).addOnFailureListener(e -> {
                // Si le fichier latest n'existe pas, essayer de charger le dernier fichier du dossier
                storage.getReference().child("schedules").listAll()
                    .addOnSuccessListener(listResult -> {
                        if (!listResult.getItems().isEmpty()) {
                            // Prendre le dernier fichier uploadé
                            StorageReference lastFile = listResult.getItems().get(listResult.getItems().size() - 1);
                            lastFile.getDownloadUrl().addOnSuccessListener(this::showImage)
                                .addOnFailureListener(ex -> showNoSchedule());
                        } else {
                            showNoSchedule();
                        }
                    })
                    .addOnFailureListener(ex -> showNoSchedule());
            });
        }).addOnFailureListener(e -> showNoSchedule());
    }

    private void showImage(Uri uri) {
        scheduleImageView.setVisibility(View.VISIBLE);
        findViewById(R.id.emptyStateContainer).setVisibility(View.GONE);
        
        Glide.with(this)
                .load(uri)
                .into(scheduleImageView);

        // Ajouter le listener pour le zoom
        scheduleImageView.setOnClickListener(v -> showZoomableImage(uri));
    }

    private void showZoomableImage(Uri imageUri) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_zoomable_image);

        PhotoView photoView = dialog.findViewById(R.id.photo_view);
        ImageView closeButton = dialog.findViewById(R.id.close_button);

        Glide.with(this)
                .load(imageUri)
                .into(photoView);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showNoSchedule() {
        scheduleImageView.setVisibility(View.GONE);
        findViewById(R.id.emptyStateContainer).setVisibility(View.VISIBLE);
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
