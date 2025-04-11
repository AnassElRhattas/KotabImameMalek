package com.example.akherapp;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static androidx.core.content.ContextCompat.startActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.akherapp.utils.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends BaseUserActivity {
    private static final String TAG = "UserProfileActivity";
    private ShapeableImageView userImageView;
    private TextView userNameText, userPhoneText, birthDateText, ageText;
    private TextInputEditText editFirstName, editLastName, editPhone, editPassword;
    private Button updateProfileButton;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private String userId;
    private NavigationView navigationView;
    private User currentUser;
    private static final int PICK_IMAGE_REQUEST = 1;
    private FloatingActionButton changePhotoButton;
    private Uri selectedImageUri;
    private StorageReference storageRef;
    private TextView userFatherNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Récupérer l'ID utilisateur depuis SharedPreferences
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("id", null);

        if (userId == null) {
            Toast.makeText(this, "الرجاء تسجيل الدخول", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setupViews();
        loadUserProfile();
        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_profile);
    }

    private void setupViews() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("الملف الشخصي");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Initialize views
        navigationView = findViewById(R.id.nav_view);
        userImageView = findViewById(R.id.userImageView);
        changePhotoButton = findViewById(R.id.changePhotoButton);
        userNameText = findViewById(R.id.userNameText);
        userPhoneText = findViewById(R.id.userPhoneText);
        birthDateText = findViewById(R.id.birthDateText);
        ageText = findViewById(R.id.ageText);
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editPhone = findViewById(R.id.editPhone);
        editPassword = findViewById(R.id.editPassword);
        updateProfileButton = findViewById(R.id.updateProfileButton);
        drawerLayout = findViewById(R.id.drawer_layout);
        userFatherNameText = findViewById(R.id.userFatherNameText);

        // Setup drawer layout
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Setup navigation view
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
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ViewScheduleActivity.class));
                finish();
            } else if (id == R.id.menu_voice_recognition) {
                startActivity(new Intent(this, VoiceRecognitionActivity.class));
            } else if (id == R.id.menu_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
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

        // Charger les données utilisateur dans le nav header
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
                                // Afficher le nom complet
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                nameView.setText(fullName.trim());
                                phoneView.setText(user.getPhone());
                                // Afficher le rôle
                                roleView.setText("طالب");

                                // Charger l'image de profil
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

        // Set click listeners
        birthDateText.setOnClickListener(v -> showDatePicker());
        updateProfileButton.setOnClickListener(v -> updateProfile());
        changePhotoButton.setOnClickListener(v -> openImagePicker());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "اختر صورة"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            uploadImage();
        }
    }

    private void uploadImage() {
        if (selectedImageUri == null) return;

        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("جاري تحميل الصورة...");
        progressDialog.show();

        // Delete old profile image if exists
        if (currentUser != null && currentUser.getProfileImageUrl() != null) {
            try {
                StorageReference oldImageRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(currentUser.getProfileImageUrl());
                oldImageRef.delete().addOnFailureListener(e -> 
                    Log.e(TAG, "Error deleting old profile image", e));
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Invalid storage URL", e);
            }
        }

        // Create a reference to the new image file
        StorageReference imageRef = storageRef.child("profile_images/" + userId + ".jpg");

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();

                        // Update user profile with new image URL
                        db.collection("users").document(userId)
                                .update("profileImageUrl", imageUrl)
                                .addOnSuccessListener(aVoid -> {
                                    // Update image view
                                    Glide.with(this)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.default_profile_image)
                                            .error(R.drawable.default_profile_image)
                                            .circleCrop()
                                            .into(userImageView);

                                    Toast.makeText(this, "تم تحديث الصورة بنجاح", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "فشل في تحديث الصورة", Toast.LENGTH_SHORT).show());
                    });
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "فشل في تحميل الصورة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setMessage("جاري التحميل " + (int)progress + "%");
                });
    }

    private void loadUserProfile() {
        Log.d(TAG, "Loading user data for ID: " + userId);

        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            Toast.makeText(this, "خطأ في تحميل معلومات المستخدم", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "Document exists in Firestore");
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            Log.d(TAG, "Successfully mapped document to User object");

                            String fullName = currentUser.getFirstName() + " " + currentUser.getLastName();
                            userNameText.setText(fullName);
                            userPhoneText.setText(currentUser.getPhone());
                            String fatherFullName = currentUser.getFatherFirstName() + " " + currentUser.getFatherLastName();
                            userFatherNameText.setText("ولي الأمر: " + fatherFullName);

                            editFirstName.setText(currentUser.getFirstName());
                            editLastName.setText(currentUser.getLastName());
                            editPhone.setText(currentUser.getPhone());
                            editPassword.setText(currentUser.getPassword());

                            if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(currentUser.getProfileImageUrl())
                                        .placeholder(R.drawable.default_profile_image)
                                        .error(R.drawable.default_profile_image)
                                        .into(userImageView);
                            } else {
                                userImageView.setImageResource(R.drawable.default_profile_image);
                            }

                            // Handle birth date conversion
                            Object birthDateValue = documentSnapshot.get("birthDate");
                            if (birthDateValue != null) {
                                currentUser.setBirthDateFromFirestore(birthDateValue);
                                updateBirthDateAndAgeDisplay();
                            } else {
                                birthDateText.setText("اضغط لتحديد تاريخ الميلاد");
                                ageText.setText("");
                            }
                        } else {
                            Log.e(TAG, "Failed to map document to User object");
                            Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Document does not exist in Firestore");
                        Toast.makeText(this, "خطأ في تحميل البيانات: المستخدم غير موجود", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    Toast.makeText(this,
                            "خطأ في تحميل البيانات: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void recreateUserDocument() {
        // Create a new user document if it doesn't exist
        User newUser = new User();
        newUser.setId(userId);

        db.collection("users").document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> loadUserProfile())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating user document", e);
                    Toast.makeText(this, "خطأ في إنشاء ملف المستخدم", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Set initial date from current birth date if available
        if (currentUser != null && currentUser.getBirthDate() != null) {
            Long birthDateLong = currentUser.getBirthDateAsLong();
            if (birthDateLong != null) {
                calendar.setTimeInMillis(birthDateLong);
            }
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar birthDate = Calendar.getInstance();
                    birthDate.set(selectedYear, selectedMonth, selectedDay);
                    birthDate.set(Calendar.HOUR_OF_DAY, 0);
                    birthDate.set(Calendar.MINUTE, 0);
                    birthDate.set(Calendar.SECOND, 0);
                    birthDate.set(Calendar.MILLISECOND, 0);

                    String birthDateMillis = String.valueOf(birthDate.getTimeInMillis());
                    updateBirthDate(birthDateMillis);
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }

    private void updateBirthDate(String birthDateMillis) {
        if (currentUser != null) {
            currentUser.setBirthDate(birthDateMillis);

            Map<String, Object> updates = new HashMap<>();
            updates.put("birthDate", birthDateMillis);

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        updateBirthDateAndAgeDisplay();
                        Toast.makeText(this, "تم تحديث تاريخ الميلاد بنجاح", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "خطأ في تحديث تاريخ الميلاد: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
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
    private void updateBirthDateAndAgeDisplay() {
        if (currentUser != null && currentUser.getBirthDate() != null) {
            // Format and display birth date
            String formattedDate = DateUtils.formatDate(currentUser.getBirthDate());
            birthDateText.setText(formattedDate);

            // Calculate and display age
            int age = DateUtils.calculateAge(currentUser.getBirthDate());
            if (age >= 0) {
                ageText.setText(age + " سنة");
            } else {
                ageText.setText("غير متوفر");
            }
        } else {
            birthDateText.setText("اضغط لتحديد تاريخ الميلاد");
            ageText.setText("");
        }
    }

    private void updateProfile() {
        String firstName = editFirstName.getText().toString().trim();
        String lastName = editLastName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String newPassword = editPassword.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("firstName", firstName);
        updates.put("lastName", lastName);
        updates.put("phone", phone);

        // Si un nouveau mot de passe est saisi, le mettre à jour
        if (!TextUtils.isEmpty(newPassword)) {
            updates.put("password", newPassword);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم تحديث المعلومات بنجاح", Toast.LENGTH_SHORT).show();
                    loadUserProfile(); // Recharger les informations
                    editPassword.setText(""); // Effacer le champ du mot de passe
                })
                .addOnFailureListener(e -> Toast.makeText(this, "فشل في تحديث المعلومات: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
