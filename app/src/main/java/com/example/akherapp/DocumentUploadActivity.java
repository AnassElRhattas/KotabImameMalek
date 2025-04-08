package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;
import static androidx.appcompat.content.res.AppCompatResources.getDrawable;
import static androidx.core.content.ContextCompat.startActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class DocumentUploadActivity extends BaseUserActivity {
    private MaterialButton btnUploadDoc1, btnUploadDoc2, btnUploadDoc3, btnSubmit;
    private Uri doc1Uri, doc2Uri, doc3Uri;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private String userId;

    private final ActivityResultLauncher<Intent> doc1Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleDocumentResult(result, uri -> doc1Uri = uri)
    );

    private final ActivityResultLauncher<Intent> doc2Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleDocumentResult(result, uri -> doc2Uri = uri)
    );

    private final ActivityResultLauncher<Intent> doc3Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> handleDocumentResult(result, uri -> doc3Uri = uri)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_upload);

        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("id", null);

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupListeners();
        checkExistingDocuments(); // Add this line
        navigationView.setCheckedItem(R.id.menu_documents);
    }

    private void checkExistingDocuments() {
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> documents = (Map<String, Object>) documentSnapshot.get("documents");
                        if (documents != null) {
                            updateButtonState("id_card", documents, btnUploadDoc1);
                            updateButtonState("photo", documents, btnUploadDoc2);
                            updateButtonState("school_certificate", documents, btnUploadDoc3);
                            
                            // Show message if any documents exist
                            if (!documents.isEmpty()) {
                                Toast.makeText(this, "لديك بعض المستندات المرفقة مسبقاً", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
        }
    }

    private TextView verificationStatusDoc1, verificationStatusDoc2, verificationStatusDoc3;

    private void initializeViews() {
        btnUploadDoc1 = findViewById(R.id.btnUploadDoc1);
        btnUploadDoc2 = findViewById(R.id.btnUploadDoc2);
        btnUploadDoc3 = findViewById(R.id.btnUploadDoc3);
        btnSubmit = findViewById(R.id.btnSubmit);
        verificationStatusDoc1 = findViewById(R.id.verificationStatusDoc1);
        verificationStatusDoc2 = findViewById(R.id.verificationStatusDoc2);
        verificationStatusDoc3 = findViewById(R.id.verificationStatusDoc3);
    }

    private void updateButtonState(String docType, Map<String, Object> documents, MaterialButton button) {
        String url = (String) documents.get(docType);
        TextView statusView = null;
        if (docType.equals("id_card")) statusView = verificationStatusDoc1;
        else if (docType.equals("photo")) statusView = verificationStatusDoc2;
        else if (docType.equals("school_certificate")) statusView = verificationStatusDoc3;

        if (url != null && !url.isEmpty()) {
            button.setText("تغيير الملف");
            // Fix the getDrawable call
            button.setIcon(getDrawable(R.drawable.ic_document));
            button.setIconGravity(MaterialButton.ICON_GRAVITY_START);

            // Rest of the method remains the same
            boolean isVerified = documents.get(docType + "_verified") != null && 
                               (boolean) documents.get(docType + "_verified");
            boolean isRejected = documents.get(docType + "_rejected") != null && 
                               (boolean) documents.get(docType + "_rejected");

            if (statusView != null) {
                statusView.setVisibility(View.VISIBLE);
                if (isVerified) {
                    statusView.setText("✓ تمت الموافقة على المستند");
                    statusView.setTextColor(getColor(R.color.green));
                } else if (isRejected) {
                    statusView.setText("✗ تم رفض المستند");
                    statusView.setTextColor(getColor(R.color.red));
                } else {
                    statusView.setText("⏳ في انتظار المراجعة");
                    statusView.setTextColor(getColor(R.color.orange));
                }
            }
        } else {
            if (statusView != null) {
                statusView.setVisibility(View.GONE);
            }
        }
    }

    private void uploadDocument(String docType, Uri uri, Runnable onSuccess) {
        if (uri == null || userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> documents = (Map<String, Object>) documentSnapshot.get("documents");
                    if (documents != null && documents.containsKey(docType)) {
                        String oldUrl = (String) documents.get(docType);
                        if (oldUrl != null) {
                            storage.getReferenceFromUrl(oldUrl).delete();
                        }
                    }

                    String path = "users/" + userId + "/documents/" + docType;
                    StorageReference ref = storage.getReference().child(path);

                    ref.putFile(uri)
                            .continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return ref.getDownloadUrl();
                            })
                            .addOnSuccessListener(downloadUri -> {
                                Map<String, Object> update = new HashMap<>();
                                update.put("documents." + docType, downloadUri.toString());

                                db.collection("users")
                                        .document(userId)
                                        .update(update)
                                        .addOnSuccessListener(aVoid -> {
                                            MaterialButton button = null;
                                            if (docType.equals("id_card")) button = btnUploadDoc1;
                                            else if (docType.equals("photo")) button = btnUploadDoc2;
                                            else if (docType.equals("school_certificate")) button = btnUploadDoc3;

                                            if (button != null) {
                                                button.setText("تغيير الملف");
                                            }
                                            onSuccess.run();
                                        })
                                        .addOnFailureListener(e -> showError("فشل في تحديث معلومات المستند"));
                            })
                            .addOnFailureListener(e -> showError("فشل في رفع المستند"));
                });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("المستندات المطلوبة");
        }
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

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
            } else if (id == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
                finish();
            } else if (id == R.id.menu_documents) {
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    private void setupListeners() {
        btnUploadDoc1.setOnClickListener(v -> selectDocument(doc1Launcher));
        btnUploadDoc2.setOnClickListener(v -> selectDocument(doc2Launcher));
        btnUploadDoc3.setOnClickListener(v -> selectDocument(doc3Launcher));
        btnSubmit.setOnClickListener(v -> uploadDocuments());
    }

    private void selectDocument(ActivityResultLauncher<Intent> launcher) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(Intent.createChooser(intent, "اختر ملفًا"));
    }

    private void handleDocumentResult(androidx.activity.result.ActivityResult result,
                                      UriConsumer uriConsumer) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            uriConsumer.accept(uri);
            btnSubmit.setEnabled(true);
        }
    }

    private void uploadDocuments() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("جاري الرفع...");

        if (doc1Uri != null) {
            uploadDocument("id_card", doc1Uri, () -> {
                if (doc2Uri != null) {
                    uploadDocument("photo", doc2Uri, () -> {
                        if (doc3Uri != null) {
                            uploadDocument("school_certificate", doc3Uri, this::updateUserProfile);
                        } else {
                            updateUserProfile();
                        }
                    });
                } else if (doc3Uri != null) {
                    uploadDocument("school_certificate", doc3Uri, this::updateUserProfile);
                } else {
                    updateUserProfile();
                }
            });
        } else if (doc2Uri != null) {
            uploadDocument("photo", doc2Uri, () -> {
                if (doc3Uri != null) {
                    uploadDocument("school_certificate", doc3Uri, this::updateUserProfile);
                } else {
                    updateUserProfile();
                }
            });
        } else if (doc3Uri != null) {
            uploadDocument("school_certificate", doc3Uri, this::updateUserProfile);
        }
    }

    private void updateUserProfile() {
        Map<String, Object> update = new HashMap<>();
        update.put("documentsSubmitted", true);

        db.collection("users")
                .document(userId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم رفع المستندات بنجاح", Toast.LENGTH_LONG).show();
                    doc1Uri = null;
                    doc2Uri = null;
                    doc3Uri = null;
                    btnSubmit.setEnabled(false);
                    btnSubmit.setText("إرسال");
                })
                .addOnFailureListener(e -> showError("فشل في تحديث الملف الشخصي"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        btnSubmit.setEnabled(true);
        btnSubmit.setText("إرسال");
    }

    private interface UriConsumer {
        void accept(Uri uri);
    }
}
