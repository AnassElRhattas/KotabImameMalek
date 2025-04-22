package com.example.akherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.AdminAdapter;
import com.example.akherapp.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AdminListActivity extends AppCompatActivity implements AdminAdapter.OnAdminClickListener {
    private RecyclerView recyclerView;
    private AdminAdapter adapter;
    private List<User> adminList = new ArrayList<>();
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        currentUserId = prefs.getString("id", null);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.adminRecyclerView);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        setupNavigationDrawer();
        
        adapter = new AdminAdapter(adminList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAdmins();
    }

    private void loadAdmins() {
        db.collection("users")
            .whereEqualTo("role", "admin")
            .addSnapshotListener((queryDocumentSnapshots, error) -> {
                if (error != null) {
                    Toast.makeText(this, "فشل في تحميل قائمة المشرفين", Toast.LENGTH_SHORT).show();
                    return;
                }
                adminList.clear();
                if (queryDocumentSnapshots != null) {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        User admin = doc.toObject(User.class);
                        if (admin != null) {
                            admin.setId(doc.getId());
                            // Ensure online status is properly set from Firestore
                            Boolean isOnline = doc.getBoolean("isOnline");
                            admin.setOnline(isOnline != null ? isOnline : false);
                            adminList.add(admin);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            });
    }

    public void onAdminClick(User admin) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("otherUserId", admin.getId());
        intent.putExtra("adminName", admin.getFirstName() + " " + admin.getLastName());
        startActivity(intent);
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ViewScheduleActivity.class));
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
            } else if (id == R.id.menu_payments) {
                startActivity(new Intent(this, PaymentActivity.class));
            } else if (id == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
            } else if (id == R.id.menu_defi_user) {
                showDevelopmentDialogRamadan();
            } else if (id == R.id.menu_chat) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_voice_recognition) {
                startActivity(new Intent(this, VoiceRecognitionActivity.class));
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_submit_complaint) {
                startActivity(new Intent(this, SubmitComplaintActivity.class));
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
        TextView nameView = headerView.findViewById(R.id.nav_header_name);
        TextView roleView = headerView.findViewById(R.id.nav_header_role);
        TextView phoneView = headerView.findViewById(R.id.nav_header_phone);
        ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                nameView.setText(fullName.trim());
                                phoneView.setText(user.getPhone());
                                roleView.setText("طالب");

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
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "خطأ في تحميل معلومات المستخدم", Toast.LENGTH_SHORT).show();
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
}