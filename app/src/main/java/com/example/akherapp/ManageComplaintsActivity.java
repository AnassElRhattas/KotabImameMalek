package com.example.akherapp;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.example.akherapp.utils.NotificationUtils;
import com.example.akherapp.utils.NotificationHelper;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.ComplaintsAdapter;
import com.example.akherapp.models.Complaint;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ManageComplaintsActivity extends AppCompatActivity implements ComplaintsAdapter.OnComplaintActionListener {
    private RecyclerView complaintsRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private ComplaintsAdapter adapter;
    private FirebaseFirestore db;
    private List<Complaint> complaints;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_complaints);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        complaintsRecyclerView = findViewById(R.id.complaintsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);

        // Setup toolbar and navigation
        setupToolbarAndNavigation();

        // Setup RecyclerView
        complaints = new ArrayList<>();
        adapter = new ComplaintsAdapter(this, complaints, this);
        complaintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        complaintsRecyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadComplaints);

        // Load complaints
        loadComplaints();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_complaints);
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
                startActivity(new Intent(this, ScheduleManagementActivity.class));
                finish();
            } else if (id == R.id.menu_users) {
                startActivity(new Intent(this, UsersListActivity.class));
                finish();
            } else if (id == R.id.menu_contact_users) {
                startActivity(new Intent(this, ContactUsersActivity.class));
                finish();
            } else if (id == R.id.menu_complaints) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_verify_documents) {
                startActivity(new Intent(this, VerifyDocumentsActivity.class));
                finish();
            }  else if (id == R.id.menu_profile) {
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
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

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
                            roleView.setText("مدير النظام");

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
    }

    private void loadComplaints() {
        swipeRefreshLayout.setRefreshing(true);
        db.collection("complaints")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                complaints.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    try {
                        Complaint complaint = document.toObject(Complaint.class);
                        complaint.setId(document.getId());
                        complaints.add(complaint);
                    } catch (Exception e) {
                        Log.e("ManageComplaints", "Error parsing complaint: " + document.getId(), e);
                    }
                }
                adapter.updateComplaints(complaints);
                updateEmptyView();
            })
            .addOnFailureListener(e -> {
                Log.e("ManageComplaints", "Error loading complaints", e);
                Toast.makeText(this, "فشل في تحميل الشكاوى: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            })
            .addOnCompleteListener(task -> {
                swipeRefreshLayout.setRefreshing(false);
            });
    }

    private void updateEmptyView() {
        if (complaints.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            complaintsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            complaintsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRespondClick(Complaint complaint) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_respond_complaint, null);
        EditText responseInput = dialogView.findViewById(R.id.responseInput);
        
        if (complaint.getAdminResponse() != null) {
            responseInput.setText(complaint.getAdminResponse());
        }

        new MaterialAlertDialogBuilder(this)
            .setTitle("الرد على الشكوى")
            .setView(dialogView)
            .setPositiveButton("إرسال", (dialog, which) -> {
                String response = responseInput.getText().toString().trim();
                if (!response.isEmpty()) {
                    updateComplaintResponse(complaint, response);
                }
            })
            .setNegativeButton("إلغاء", null)
            .show();
    }

    @Override
    public void onMarkAsInProgressClick(Complaint complaint) {
        updateComplaintStatus(complaint, "in_progress");
    }

    @Override
    public void onMarkAsResolvedClick(Complaint complaint) {
        updateComplaintStatus(complaint, "resolved");
    }

    @Override
    public void onDeleteClick(Complaint complaint) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("حذف الشكوى")
            .setMessage("هل أنت متأكد من حذف هذه الشكوى؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                deleteComplaint(complaint);
            })
            .setNegativeButton("لا", null)
            .show();
    }

    private void updateComplaintStatus(Complaint complaint, String newStatus) {
        db.collection("complaints")
            .document(complaint.getId())
            .update(
                "status", newStatus,
                "updatedAt", com.google.firebase.Timestamp.now()
            )
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "تم تحديث حالة الشكوى", Toast.LENGTH_SHORT).show();
                loadComplaints();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "فشل في تحديث حالة الشكوى", Toast.LENGTH_SHORT).show()
            );
    }

    private void updateComplaintResponse(Complaint complaint, String response) {
        db.collection("complaints")
                .document(complaint.getId())
                .update(
                        "adminResponse", response,
                        "updatedAt", com.google.firebase.Timestamp.now()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم إرسال الرد بنجاح", Toast.LENGTH_SHORT).show();
                    loadComplaints();

                    // Envoi de la notification à l'utilisateur concerné
                    db.collection("users").document(complaint.getUserId())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                List<String> fcmTokens = (List<String>) documentSnapshot.get("fcmTokens");

                                if (fcmTokens != null && !fcmTokens.isEmpty()) {
                                    String title = "تم الرد على شكواك";
                                    String message = "تمت معالجة شكواك. رد الإدارة: " + response;

                                    for (String token : fcmTokens) {
                                        NotificationUtils.sendNotification(token, title, message);
                                    }

                                    // Sauvegarder la notification dans Firestore
                                    NotificationHelper.saveNotification(complaint.getUserId(), title, message);
                                } else {
                                    Log.w("ComplaintResponse", "Aucun token FCM trouvé pour l'utilisateur: " + complaint.getUserId());
                                }
                            })
                            .addOnFailureListener(e ->
                                    Log.e("ComplaintResponse", "Erreur lors de la récupération des tokens FCM", e)
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل في إرسال الرد", Toast.LENGTH_SHORT).show()
                );
    }


    private void deleteComplaint(Complaint complaint) {
        db.collection("complaints")
            .document(complaint.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "تم حذف الشكوى بنجاح", Toast.LENGTH_SHORT).show();
                loadComplaints();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "فشل في حذف الشكوى", Toast.LENGTH_SHORT).show()
            );
    }

    private void deleteAllComplaints() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("حذف جميع الشكاوى")
            .setMessage("هل أنت متأكد من حذف جميع الشكاوى؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                db.collection("complaints")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            document.getReference().delete();
                        }
                        Toast.makeText(this, "تم حذف جميع الشكاوى بنجاح", Toast.LENGTH_SHORT).show();
                        loadComplaints();
                    })
                    .addOnFailureListener(e -> 
                        Toast.makeText(this, "فشل في حذف الشكاوى", Toast.LENGTH_SHORT).show()
                    );
            })
            .setNegativeButton("لا", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_complaints_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete_all) {
            deleteAllComplaints();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
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