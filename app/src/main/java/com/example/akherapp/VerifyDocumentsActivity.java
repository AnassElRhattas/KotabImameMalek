package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.UserDocumentsAdapter;
import com.example.akherapp.User;
import com.example.akherapp.utils.NotificationUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.akherapp.utils.NotificationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VerifyDocumentsActivity extends BaseActivity implements UserDocumentsAdapter.OnDocumentActionListener {
    private RecyclerView usersRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private UserDocumentsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private List<User> users;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_documents);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize views and setup
        initializeViews();
        setupToolbarAndNavigation();

        // Setup RecyclerView
        users = new ArrayList<>();
        adapter = new UserDocumentsAdapter(this, users, this);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(adapter);

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadUsers);

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_verify_documents);
        // Load users
        loadUsers();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
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
            } else if (id == R.id.menu_verify_documents) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_users) {
                startActivity(new Intent(this, UsersListActivity.class));
                finish();
            } else if (id == R.id.menu_contact_users) {
                startActivity(new Intent(this, ContactUsersActivity.class));
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ScheduleManagementActivity.class));
            } else if (id == R.id.menu_complaints) {
                startActivity(new Intent(this, ManageComplaintsActivity.class));
                finish();
            }  else if (id == R.id.menu_add_news) {
                startActivity(new Intent(this, AdminActivity.class));
                finish();}
            else if (id == R.id.menu_profile) {
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

    private void loadUsers() {
        swipeRefreshLayout.setRefreshing(true);
        db.collection("users")
            .whereEqualTo("documentsSubmitted", true)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                users.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    user.setId(document.getId());
                    users.add(user);
                }
                adapter.notifyDataSetChanged();
                updateEmptyView();
                swipeRefreshLayout.setRefreshing(false);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في تحميل المستخدمين", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
            });
    }

    private void updateEmptyView() {
        if (users.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            usersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            usersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onViewDocument(String documentUrl) {
        if (documentUrl != null && !documentUrl.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(documentUrl), "*/*");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "اختر تطبيق لفتح المستند"));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "لا يوجد تطبيق مناسب لفتح هذا المستند", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "المستند غير متوفر", Toast.LENGTH_SHORT).show();
        }
    }

    // Remove these methods

    // Add these new methods
    @Override
    public void onApproveDocument(User user, String documentType) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("تأكيد الموافقة")
            .setMessage("هل أنت متأكد من الموافقة على هذا المستند؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                // Appeler updateDocumentStatus au lieu de faire la mise à jour directement
                updateDocumentStatus(user, documentType, true);
                
                // Vérifier si tous les documents sont validés après la mise à jour
                checkAllDocumentsVerified(user);
            })
            .setNegativeButton("لا", null)
            .show();
    }

    private void updateDocumentStatus(User user, String documentType, boolean isApproved) {
        // Mettre à jour le statut dans Firestore
        db.collection("users").document(user.getId())
            .update(
                "documents." + documentType + "_verified", isApproved,
                "documents." + documentType + "_rejected", !isApproved,
                "documentsVerified", isApproved,  // Ajouter ces champs
                "documentsRejected", !isApproved  // pour la synchronisation
            )
            .addOnSuccessListener(aVoid -> {
                String message = isApproved ? 
                    "تمت الموافقة على المستند" : 
                    "تم رفض المستند";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                
                // Mettre à jour l'objet User local
                user.setDocumentVerified(documentType, isApproved);
                user.setDocumentRejected(documentType, !isApproved);
                
                // Rafraîchir la liste
                loadUsers();
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "فشل في تحديث حالة المستند", Toast.LENGTH_SHORT).show()
            );
    }

    @Override
    public void onRejectDocument(User user, String documentType) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("تأكيد الرفض")
            .setMessage("هل أنت متأكد من رفض هذا المستند؟")
            .setPositiveButton("نعم", (dialog, which) -> {
                updateDocumentStatus(user, documentType, false);
            })
            .setNegativeButton("لا", null)
            .show();
    }

//    private void updateDocumentStatus(User user, String documentType, boolean isApproved) {
//        db.collection("users").document(user.getId())
//            .update(
//                "documents." + documentType + "_verified", isApproved,
//                "documents." + documentType + "_rejected", !isApproved
//            )
//            .addOnSuccessListener(aVoid -> {
//                String message = isApproved ?
//                    "تمت الموافقة على المستند" :
//                    "تم رفض المستند";
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//                loadUsers();
//            })
//            .addOnFailureListener(e ->
//                Toast.makeText(this, "فشل في تحديث حالة المستند", Toast.LENGTH_SHORT).show()
//            );
//    }

    private void checkAllDocumentsVerified(User user) {
        // Get fresh user data to ensure we have the latest FCM tokens
        db.collection("users").document(user.getId())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                Map<String, Object> documents = (Map<String, Object>) documentSnapshot.get("documents");
                List<String> fcmTokens = (List<String>) documentSnapshot.get("fcmTokens");
                
                Log.d("VerifyDocuments", "Checking documents for user: " + user.getId());
                Log.d("VerifyDocuments", "Documents: " + documents);
                Log.d("VerifyDocuments", "FCM Tokens: " + fcmTokens);

                boolean allVerified = documents != null &&
                    Boolean.TRUE.equals(documents.get("id_card_verified")) &&
                    Boolean.TRUE.equals(documents.get("photo_verified")) &&
                    Boolean.TRUE.equals(documents.get("school_certificate_verified")) &&
                    !Boolean.TRUE.equals(documents.get("id_card_rejected")) &&
                    !Boolean.TRUE.equals(documents.get("photo_rejected")) &&
                    !Boolean.TRUE.equals(documents.get("school_certificate_rejected"));

                if (allVerified) {
                    Log.d("VerifyDocuments", "All documents verified for user: " + user.getId());
                    
                    // Update user status first
                    db.collection("users").document(user.getId())
                        .update(
                            "documentsVerified", true,
                            "registrationCompletionNotified", true,
                            "registrationCompleted", true
                        )
                        .addOnSuccessListener(aVoid -> {
                            if (fcmTokens != null && !fcmTokens.isEmpty()) {
                                for (String token : fcmTokens) {
                                    NotificationUtils.sendNotification(
                                        token,
                                        "تهانينا!",
                                        "تم التحقق من جميع مستنداتك بنجاح. اكتمل تسجيلك!"
                                    );
                                    Log.d("VerifyDocuments", "Notification sent to token: " + token);
                                }
                                
                                // Save notification to Firestore
                                NotificationHelper.saveNotification(
                                    user.getId(),
                                    "تهانينا!",
                                    "تم التحقق من جميع مستنداتك بنجاح. اكتمل تسجيلك!"
                                );
                            } else {
                                Log.w("VerifyDocuments", "No FCM tokens found for user: " + user.getId());
                            }
                        })
                        .addOnFailureListener(e -> 
                            Log.e("VerifyDocuments", "Error updating user status", e)
                        );
                } else {
                    Log.d("VerifyDocuments", "Not all documents are verified yet for user: " + user.getId());
                }
            })
            .addOnFailureListener(e -> 
                Log.e("VerifyDocuments", "Error fetching user data", e)
            );
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