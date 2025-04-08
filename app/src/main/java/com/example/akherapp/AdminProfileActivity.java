package com.example.akherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class AdminProfileActivity extends BaseActivity {
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextInputEditText firstNameInput, lastNameInput, phoneInput, passwordInput;
    private ShapeableImageView profileImageView;
    private Uri selectedImageUri;
    private String currentUserId;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        
        // Initialiser les vues
        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupImagePicker();
        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_profile);
        loadAdminData();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        passwordInput = findViewById(R.id.passwordInput);
        profileImageView = findViewById(R.id.profileImageView);
        Button changePhotoButton = findViewById(R.id.changePhotoButton);
        Button saveButton = findViewById(R.id.saveButton);

        changePhotoButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        saveButton.setOnClickListener(v -> saveChanges());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("الملف الشخصي");
        }
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
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
                finish();
            } else if (id == R.id.menu_profile) {
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
            } else if (id == R.id.menu_add_news) {
                startActivity(new Intent(this, AdminActivity.class));
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
        currentUserId = prefs.getString("id", null);

        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            TextView nameView = headerView.findViewById(R.id.nav_header_name);
                            TextView roleView = headerView.findViewById(R.id.nav_header_role);
                            ShapeableImageView headerImageView = headerView.findViewById(R.id.nav_header_image);

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
                            if (headerImageView != null) {
                                String imageUrl = user.getProfileImageUrl();
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.default_profile_image)
                                        .error(R.drawable.default_profile_image)
                                        .circleCrop()
                                        .into(headerImageView);
                                } else {
                                    headerImageView.setImageResource(R.drawable.default_profile_image);
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

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(profileImageView);
                }
            });
    }

    private void loadAdminData() {
        if (currentUserId != null) {
            db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            firstNameInput.setText(user.getFirstName());
                            lastNameInput.setText(user.getLastName());
                            phoneInput.setText(user.getPhone());
                            passwordInput.setText(user.getPassword());

                            String imageUrl = user.getProfileImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.default_profile_image)
                                    .error(R.drawable.default_profile_image)
                                    .circleCrop()
                                    .into(profileImageView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void saveChanges() {
        if (currentUserId == null) return;

        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("جاري حفظ التغييرات...");
        progressDialog.show();

        // Get current user data to check for existing profile image
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener(documentSnapshot -> {
                User currentUser = documentSnapshot.toObject(User.class);
                
                // If new image selected, handle image upload
                if (selectedImageUri != null) {
                    // Delete old profile image if exists
                    if (currentUser != null && currentUser.getProfileImageUrl() != null) {
                        try {
                            StorageReference oldImageRef = storage
                                .getReferenceFromUrl(currentUser.getProfileImageUrl());
                            oldImageRef.delete().addOnFailureListener(e -> 
                                Log.e("AdminProfile", "Error deleting old profile image", e));
                        } catch (IllegalArgumentException e) {
                            Log.e("AdminProfile", "Invalid storage URL", e);
                        }
                    }

                    // Upload new image
                    StorageReference imageRef = storage.getReference()
                        .child("profile_images/" + currentUserId + ".jpg");
                    
                    imageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> {
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update user with new image URL
                                updateUserProfile(firstName, lastName, phone, password, uri.toString(), progressDialog);
                            });
                        })
                        .addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(AdminProfileActivity.this, 
                                "فشل في تحميل الصورة", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    // Update user without changing image
                    updateUserProfile(firstName, lastName, phone, password, 
                        currentUser != null ? currentUser.getProfileImageUrl() : null, 
                        progressDialog);
                }
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "فشل في تحديث البيانات", Toast.LENGTH_SHORT).show();
            });
    }

    private void updateUserProfile(String firstName, String lastName, String phone, 
            String password, String imageUrl, android.app.ProgressDialog progressDialog) {
        User updatedUser = new User();
        updatedUser.setFirstName(firstName);
        updatedUser.setLastName(lastName);
        updatedUser.setPhone(phone);
        updatedUser.setPassword(password);
        updatedUser.setRole("admin");
        updatedUser.setProfileImageUrl(imageUrl);

        db.collection("users").document(currentUserId)
            .set(updatedUser)
            .addOnSuccessListener(aVoid -> {
                progressDialog.dismiss();
                Toast.makeText(this, "تم تحديث البيانات بنجاح", Toast.LENGTH_SHORT).show();
                loadAdminData(); // Reload the profile data
            })
            .addOnFailureListener(e -> {
                progressDialog.dismiss();
                Toast.makeText(this, "فشل في تحديث البيانات", Toast.LENGTH_SHORT).show();
            });
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
