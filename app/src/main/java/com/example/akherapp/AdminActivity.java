package com.example.akherapp;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static androidx.core.content.ContextCompat.startActivity;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.example.akherapp.adapters.SelectedVideosAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.example.akherapp.utils.NotificationUtils;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminActivity extends BaseActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_VIDEO_REQUEST = 2;
    private ImageView newsImageView;
    private TextInputEditText titleInput, descriptionInput;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView selectedImagesRecyclerView;
    private SelectedImagesAdapter selectedImagesAdapter;
    private RecyclerView selectedVideosRecyclerView;
    private SelectedVideosAdapter selectedVideosAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initializeViews();
        setupViews();
        setupNavigationDrawer();
        setupListeners();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_add_news);
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        selectedImagesRecyclerView = findViewById(R.id.selectedImagesRecyclerView);
        selectedImagesRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImagesAdapter = new SelectedImagesAdapter(position ->
                selectedImagesAdapter.removeImage(position));
        selectedImagesRecyclerView.setAdapter(selectedImagesAdapter);
        selectedVideosRecyclerView = findViewById(R.id.selectedVideosRecyclerView);
        selectedVideosRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedVideosAdapter = new SelectedVideosAdapter(position ->
                selectedVideosAdapter.removeVideo(position));
        selectedVideosRecyclerView.setAdapter(selectedVideosAdapter);
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("إضافة خبر جديد");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

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
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
                finish();
            } else if (id == R.id.menu_add_news) {
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
            }  else if (id == R.id.menu_verify_documents) {
                startActivity(new Intent(this, VerifyDocumentsActivity.class));
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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupListeners() {
        findViewById(R.id.btnSelectImage).setOnClickListener(v -> selectImage());
        findViewById(R.id.btnSelectVideo).setOnClickListener(v -> selectVideo());
        findViewById(R.id.btnPublish).setOnClickListener(v -> publishNews());
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "اختر الصور"), PICK_IMAGE_REQUEST);
    }

    private void selectVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "اختر الفيديوهات"), PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                handleImageSelection(data);
            } else if (requestCode == PICK_VIDEO_REQUEST) {
                handleVideoSelection(data);
            }
        }
    }

    private void handleVideoSelection(Intent data) {
        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri videoUri = clipData.getItemAt(i).getUri();
                selectedVideosAdapter.addVideo(videoUri);
            }
        } else if (data.getData() != null) {
            selectedVideosAdapter.addVideo(data.getData());
        }
    }

    private void handleImageSelection(Intent data) {
        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri imageUri = clipData.getItemAt(i).getUri();
                selectedImagesAdapter.addImage(imageUri);
            }
        } else if (data.getData() != null) {
            selectedImagesAdapter.addImage(data.getData());
        }
    }

    private void publishNews() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        List<Uri> selectedImages = selectedImagesAdapter.getImageUris();
        List<Uri> selectedVideos = selectedVideosAdapter.getVideoUris();

        if (title.isEmpty() || description.isEmpty() ||
                (selectedImages.isEmpty() && selectedVideos.isEmpty())) {
            Toast.makeText(this, "الرجاء إدخال جميع البيانات المطلوبة", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري نشر الخبر...");
        progressDialog.show();

        uploadMedia(selectedImages, selectedVideos, new ArrayList<>(), new ArrayList<>(),
                progressDialog, title, description);
    }

    private void uploadImage(List<Uri> remainingImages, List<Uri> remainingVideos,
                             List<String> uploadedImageUrls, List<String> uploadedVideoUrls,
                             ProgressDialog progressDialog, String title, String description) {
        if (remainingImages.isEmpty()) {
            uploadMedia(remainingImages, remainingVideos, uploadedImageUrls,
                    uploadedVideoUrls, progressDialog, title, description);
            return;
        }

        Uri imageUri = remainingImages.get(0);
        List<Uri> newRemainingImages = remainingImages.subList(1, remainingImages.size());

        StorageReference imageRef = storage.getReference()
                .child("news_images")
                .child(UUID.randomUUID().toString());

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                uploadedImageUrls.add(uri.toString());
                                uploadMedia(newRemainingImages, remainingVideos, uploadedImageUrls,
                                        uploadedVideoUrls, progressDialog, title, description);
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(AdminActivity.this,
                                        "فشل في تحميل الصورة", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AdminActivity.this,
                            "فشل في تحميل الصورة", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadVideo(List<Uri> remainingVideos, List<String> uploadedImageUrls,
                             List<String> uploadedVideoUrls, ProgressDialog progressDialog,
                             String title, String description) {
        if (remainingVideos.isEmpty()) {
            uploadMedia(new ArrayList<>(), remainingVideos, uploadedImageUrls,
                    uploadedVideoUrls, progressDialog, title, description);
            return;
        }

        Uri videoUri = remainingVideos.get(0);
        List<Uri> newRemainingVideos = remainingVideos.subList(1, remainingVideos.size());

        StorageReference videoRef = storage.getReference()
                .child("news_videos")
                .child(UUID.randomUUID().toString());

        videoRef.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    videoRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                uploadedVideoUrls.add(uri.toString());
                                uploadMedia(new ArrayList<>(), newRemainingVideos, uploadedImageUrls,
                                        uploadedVideoUrls, progressDialog, title, description);
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(AdminActivity.this,
                                        "فشل في تحميل الفيديو", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(AdminActivity.this,
                            "فشل في تحميل الفيديو", Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadMedia(List<Uri> remainingImages, List<Uri> remainingVideos,
                             List<String> uploadedImageUrls, List<String> uploadedVideoUrls,
                             ProgressDialog progressDialog, String title, String description) {
        if (!remainingImages.isEmpty()) {
            uploadImage(remainingImages, remainingVideos, uploadedImageUrls,
                    uploadedVideoUrls, progressDialog, title, description);
        } else if (!remainingVideos.isEmpty()) {
            uploadVideo(remainingVideos, uploadedImageUrls, uploadedVideoUrls,
                    progressDialog, title, description);
        } else {
            saveNewsToFirestore(title, description, uploadedImageUrls,
                    uploadedVideoUrls, progressDialog);
        }
    }

    private void saveNewsToFirestore(String title, String description,
                                     List<String> imageUrls, List<String> videoUrls,
                                     ProgressDialog progressDialog) {
        News news = new News(title, description, imageUrls, videoUrls);
        String newsId = UUID.randomUUID().toString();
        news.setId(newsId);

        db.collection("news").document(newsId)
                .set(news)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "تم نشر الخبر بنجاح", Toast.LENGTH_SHORT).show();
                    NotificationUtils.sendNotificationToAllUsers("خبر جديد", title);
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "فشل في نشر الخبر", Toast.LENGTH_SHORT).show();
                });
    }

    private void clearForm() {
        titleInput.setText("");
        descriptionInput.setText("");
        selectedImagesAdapter = new SelectedImagesAdapter(position ->
                selectedImagesAdapter.removeImage(position));
        selectedImagesRecyclerView.setAdapter(selectedImagesAdapter);
    }
}