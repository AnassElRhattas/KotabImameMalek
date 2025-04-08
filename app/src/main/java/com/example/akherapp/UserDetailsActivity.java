package com.example.akherapp;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.example.akherapp.utils.DateUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.akherapp.CertificateGenerator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import android.content.ActivityNotFoundException;
import android.app.ProgressDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserDetailsActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private TextView userFullNameText, userPhoneText, userBirthDate, userAge, teacherText;
    private ImageView userImageView;
    private EditText week1Input, week2Input, week3Input, week4Input;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage; // Add this field
    private String userId;
    private TextView userFatherNameText;
    private TextView idCardStatus, photoStatus, certificateStatus;
    private User user;
    private Button generateCertificateButton;
    private Button viewCertificateButton;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(); // Initialize FirebaseStorage

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            startActivity(new Intent(this, UsersListActivity.class));
            finish();
        });

        userFullNameText = findViewById(R.id.userFullNameText);
        userPhoneText = findViewById(R.id.userPhoneText);
        userBirthDate = findViewById(R.id.userBirthDate);
        userAge = findViewById(R.id.userAge);
        teacherText = findViewById(R.id.teacherText);
        userImageView = findViewById(R.id.userImageView);
        userFatherNameText = findViewById(R.id.userFatherNameText);

        idCardStatus = findViewById(R.id.idCardStatus);
        photoStatus = findViewById(R.id.photoStatus);
        certificateStatus = findViewById(R.id.certificateStatus);

        week1Input = findViewById(R.id.week1Value);
        week2Input = findViewById(R.id.week2Value);
        week3Input = findViewById(R.id.week3Value);
        week4Input = findViewById(R.id.week4Value);

        userId = getIntent().getStringExtra("USER_ID");
        if (userId != null) {
            loadUserData();
        } else {
            Toast.makeText(this, "خطأ: معرف المستخدم غير موجود", Toast.LENGTH_SHORT).show();
            finish();
        }

        findViewById(R.id.saveButton).setOnClickListener(v -> saveProgress());
        findViewById(R.id.deleteUserButton).setOnClickListener(v -> confirmDelete());

        storage = FirebaseStorage.getInstance();
        generateCertificateButton = findViewById(R.id.generateCertificateButton);
        viewCertificateButton = findViewById(R.id.viewCertificateButton);
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري إنشاء الشهادة...");
        progressDialog.setCancelable(false);

        generateCertificateButton.setOnClickListener(v -> generateUserCertificate());
        viewCertificateButton.setOnClickListener(v -> viewCertificate());
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("تأكيد الحذف")
                .setMessage("هل أنت متأكد من حذف هذا المستخدم؟")
                .setPositiveButton("حذف", (dialog, which) -> deleteUser())
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteUser() {
        db.collection("users").document(userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم حذف المستخدم بنجاح", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, UsersListActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في حذف المستخدم", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            displayUserDetails(user);

                            week1Input.setText(String.valueOf(user.getWeek1Progress() != null ? user.getWeek1Progress() : 0));
                            week2Input.setText(String.valueOf(user.getWeek2Progress() != null ? user.getWeek2Progress() : 0));
                            week3Input.setText(String.valueOf(user.getWeek3Progress() != null ? user.getWeek3Progress() : 0));
                            week4Input.setText(String.valueOf(user.getWeek4Progress() != null ? user.getWeek4Progress() : 0));

                            // Documents
                            Map<String, Object> documents = (Map<String, Object>) documentSnapshot.get("documents");
                            if (documents != null) {
                                updateDocumentStatus("id_card", documents, idCardStatus);
                                updateDocumentStatus("photo", documents, photoStatus);
                                updateDocumentStatus("school_certificate", documents, certificateStatus);
                            }

                            // Vérifier si le certificat est déjà généré
                            Map<String, Object> docs = user.getDocuments();
                            boolean hasCertificate = (docs != null && docs.containsKey("certificateUrl"))
                                    || (user.getCertificateUrl() != null && !user.getCertificateUrl().isEmpty());

                            generateCertificateButton.setVisibility(hasCertificate ? View.GONE : View.VISIBLE);
                            viewCertificateButton.setVisibility(hasCertificate ? View.VISIBLE : View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "المستخدم غير موجود", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل في تحميل بيانات المستخدم", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }


    private void displayUserDetails(User user) {
        if (user != null) {
            String fullName = user.getFirstName() + " " + user.getLastName();
            userFullNameText.setText(fullName);
            userPhoneText.setText(user.getPhone());

            if (user.getBirthDate() != null) {
                String formattedDate = DateUtils.formatDate(user.getBirthDate());
                userBirthDate.setText("تاريخ الميلاد: " + formattedDate);
                int age = DateUtils.calculateAge(user.getBirthDate());
                userAge.setText("العمر: " + (age >= 0 ? age + " سنة" : "غير متوفر"));
            } else {
                userBirthDate.setText("تاريخ الميلاد: غير متوفر");
                userAge.setText("العمر: غير متوفر");
            }

            if (user.getTeacher() != null && !user.getTeacher().isEmpty()) {
                teacherText.setText(user.getTeacher());
                teacherText.setVisibility(View.VISIBLE);
            } else {
                teacherText.setVisibility(View.GONE);
            }

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(userImageView);
            } else {
                userImageView.setImageResource(R.drawable.default_profile);
            }

            String fatherFullName = user.getFatherFirstName() + " " + user.getFatherLastName();
            userFatherNameText.setText(android.text.Html.fromHtml("<b>ولي الأمر</b> : " + fatherFullName));
        }
    }

    private void saveProgress() {
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("week1Progress", Float.parseFloat(week1Input.getText().toString()));
            updates.put("week2Progress", Float.parseFloat(week2Input.getText().toString()));
            updates.put("week3Progress", Float.parseFloat(week3Input.getText().toString()));
            updates.put("week4Progress", Float.parseFloat(week4Input.getText().toString()));

            db.collection("users").document(userId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "تم حفظ التقدم بنجاح", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "فشل في حفظ التقدم", Toast.LENGTH_SHORT).show();
                    });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "الرجاء إدخال أرقام صحيحة", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDocumentStatus(String docType, Map<String, Object> documents, TextView statusView) {
        String url = (String) documents.get(docType);
        if (url != null && !url.isEmpty()) {
            statusView.setText("تم الرفع");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusView.setText("غير متوفر");
            statusView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    private void generateUserCertificate() {
        progressDialog.show();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                File certificate = CertificateGenerator.generateCertificate(this, user);
                handler.post(() -> {
                    progressDialog.dismiss();
                    loadUserData(); // Refresh to show view button
                    Toast.makeText(this, "تم إنشاء الشهادة بنجاح", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "فشل في إنشاء الشهادة", Toast.LENGTH_SHORT).show();
                });
                e.printStackTrace();
            }
        });
    }

    private void viewCertificate() {
        if (user != null) {
            // Check in documents first
            Map<String, Object> documents = user.getDocuments();
            String certificateUrl = null;
            
            if (documents != null && documents.containsKey("certificateUrl")) {
                certificateUrl = (String) documents.get("certificateUrl");
            } else {
                certificateUrl = user.getCertificateUrl();
            }

            if (certificateUrl != null && !certificateUrl.isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(certificateUrl), "application/pdf");
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "اختر تطبيق لفتح الشهادة"));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "لا يوجد تطبيق لفتح ملف PDF", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "الشهادة غير متوفرة", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCertificateFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = FileProvider.getUriForFile(this, 
            "com.example.akherapp.fileprovider", file);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "لا يوجد تطبيق لفتح ملف PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
