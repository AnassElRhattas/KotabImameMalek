package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.ScheduleUpdateAdapter;
import com.example.akherapp.models.ScheduleUpdate;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScheduleManagementActivity extends BaseActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ImageView schedulePreview;
    private ProgressBar uploadProgress;
    private Button uploadButton;
    private Uri selectedFileUri;
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_management);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupFilePicker();
        loadCurrentSchedule();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_schedule);
    }

    // Après les variables de classe existantes
    private TextView totalViewsCount;
    private TextView lastUpdateText;
    private RecyclerView updateHistoryRecyclerView;

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        schedulePreview = findViewById(R.id.previewImageView);
        uploadProgress = findViewById(R.id.uploadProgressBar);
        uploadButton = findViewById(R.id.uploadButton);
    
        // Add these lines
        totalViewsCount = findViewById(R.id.totalViewsCount);
        lastUpdateText = findViewById(R.id.lastUpdateText);
        updateHistoryRecyclerView = findViewById(R.id.updateHistoryRecyclerView);
        
        // Initialize RecyclerView
        setupUpdateHistory();
        
        // Load initial data
        loadStatistics();

        uploadButton.setOnClickListener(v -> selectFile());
        
        // Ajouter le listener pour le zoom de l'image
        schedulePreview.setOnClickListener(v -> {
            if (selectedFileUri != null || schedulePreview.getDrawable() != null) {
                Intent intent = new Intent(this, ImageViewerActivity.class);
                if (selectedFileUri != null) {
                    intent.putExtra("imageUri", selectedFileUri.toString());
                } else {
                    // Pour l'image déjà chargée
                    intent.putExtra("imageUrl", "schedules/schedule_latest");
                }
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
                finish();
            } else if (id == R.id.menu_add_news) {
                startActivity(new Intent(this, AdminActivity.class));
                finish();
            } else if (id == R.id.menu_schedule) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_users) {
                startActivity(new Intent(this, UsersListActivity.class));
                finish();
            } else if (id == R.id.menu_verify_documents) {
                startActivity(new Intent(this, VerifyDocumentsActivity.class));
                finish();
            }  else if (id == R.id.menu_contact_users) {
                startActivity(new Intent(this, ContactUsersActivity.class));
            } else if (id == R.id.menu_complaints) {
                startActivity(new Intent(this, ManageComplaintsActivity.class));
                finish();
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, AdminProfileActivity.class));
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

        // Charger les informations de l'utilisateur dans l'en-tête
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

                                // Afficher le nom complet
                                String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                        (user.getLastName() != null ? user.getLastName() : "");
                                if (nameView != null) {
                                    nameView.setText(fullName.trim());
                                }

                                // Afficher le rôle
                                if (roleView != null) {
                                    roleView.setText("مدير النظام");
                                }

                                // Charger l'image de profil
                                if (profileImageView != null) {
                                    String imageUrl = user.getProfileImageUrl();
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        Glide.with(this)
                                                .load(imageUrl)
                                                .placeholder(R.drawable.default_profile_image)
                                                .error(R.drawable.default_profile_image)
                                                .circleCrop()
                                                .into(profileImageView);
                                    } else {
                                        profileImageView.setImageResource(R.drawable.default_profile_image);
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "خطأ في تحميل معلومات المستخدم",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    // Si c'est une image, afficher l'aperçu
                    String mimeType = getContentResolver().getType(uri);
                    if (mimeType != null && mimeType.startsWith("image/")) {
                        schedulePreview.setVisibility(View.VISIBLE);
                        Glide.with(this)
                            .load(uri)
                            .into(schedulePreview);
                    } else {
                        schedulePreview.setVisibility(View.GONE);
                    }
                    uploadFile();
                }
            }
        );
    }

    private void selectFile() {
        filePickerLauncher.launch("image/*");
    }

    private void uploadFile() {
        if (selectedFileUri == null) return;

        uploadProgress.setVisibility(View.VISIBLE);
        uploadButton.setEnabled(false);

        // Créer une référence pour le nouveau fichier
        String fileName = "schedule_" + System.currentTimeMillis();
        StorageReference newScheduleRef = storage.getReference().child("schedules").child(fileName);
        StorageReference latestRef = storage.getReference().child("schedules").child("schedule_latest");

        newScheduleRef.putFile(selectedFileUri)
            .addOnProgressListener(snapshot -> {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                uploadProgress.setProgress((int) progress);
            })
            .addOnSuccessListener(taskSnapshot -> {
                // Une fois le fichier uploadé, copier vers schedule_latest
                newScheduleRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Mettre à jour la référence latest
                    latestRef.putFile(selectedFileUri)
                        .addOnSuccessListener(task -> {
                            uploadProgress.setVisibility(View.GONE);
                            uploadButton.setEnabled(true);
                            Toast.makeText(ScheduleManagementActivity.this,
                                "تم تحميل الجدول بنجاح",
                                Toast.LENGTH_SHORT).show();
                            
                            // Add this line
                            updateStatistics(uri.toString());
                            
                            loadCurrentSchedule();
                            // Add these lines
                            loadStatistics();
                            loadUpdateHistory();
                        })
                        .addOnFailureListener(e -> {
                            uploadProgress.setVisibility(View.GONE);
                            uploadButton.setEnabled(true);
                            Toast.makeText(ScheduleManagementActivity.this,
                                "خطأ في تحميل الجدول",
                                Toast.LENGTH_SHORT).show();
                        });
                });
            })
            .addOnFailureListener(e -> {
                uploadProgress.setVisibility(View.GONE);
                uploadButton.setEnabled(true);
                Toast.makeText(ScheduleManagementActivity.this,
                    "خطأ في تحميل الجدول",
                    Toast.LENGTH_SHORT).show();
            });
    }

    private void loadCurrentSchedule() {
        StorageReference scheduleRef = storage.getReference().child("schedules").child("schedule_latest");
        scheduleRef.getDownloadUrl()
            .addOnSuccessListener(uri -> {
                schedulePreview.setVisibility(View.VISIBLE);
                Glide.with(this)
                    .load(uri)
                    .into(schedulePreview);
            })
            .addOnFailureListener(e -> schedulePreview.setVisibility(View.GONE));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupUpdateHistory() {
        updateHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        loadUpdateHistory();
    }

    private void loadStatistics() {
        db.collection("schedule_stats").document("latest")
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Long views = documentSnapshot.getLong("views");
                    String lastUpdate = documentSnapshot.getString("lastUpdate");
                    
                    if (views != null) {
                        totalViewsCount.setText(String.valueOf(views));
                    }
                    
                    if (lastUpdate != null) {
                        lastUpdateText.setText(lastUpdate);
                    }
                }
            });
    }

    private void loadUpdateHistory() {
        db.collection("schedule_updates")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ScheduleUpdate> updates = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ScheduleUpdate update = document.toObject(ScheduleUpdate.class);
                    updates.add(update);
                }
                
                ScheduleUpdateAdapter adapter = new ScheduleUpdateAdapter(updates, this::viewHistoricalSchedule);
                updateHistoryRecyclerView.setAdapter(adapter);
            });
    }

    private void viewHistoricalSchedule(String scheduleUrl) {
        Intent intent = new Intent(this, ImageViewerActivity.class);
        intent.putExtra("imageUrl", scheduleUrl);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics();
    }

    // Dans la méthode uploadFile(), après le succès de l'upload
    private void updateStatistics(String newScheduleUrl) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("lastUpdate", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        stats.put("views", 0L);

        db.collection("schedule_stats").document("latest")
            .set(stats, SetOptions.merge());

        // Ajouter à l'historique
        Map<String, Object> updateRecord = new HashMap<>();
        updateRecord.put("timestamp", new Date());
        updateRecord.put("scheduleUrl", newScheduleUrl);

        db.collection("schedule_updates")
            .add(updateRecord);
    }
}
