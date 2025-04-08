package com.example.akherapp;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.chrisbanes.photoview.PhotoView;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        PhotoView photoView = findViewById(R.id.photo_view);
        ImageView closeButton = findViewById(R.id.close_button);

        String imageUri = getIntent().getStringExtra("imageUri");
        String imageUrl = getIntent().getStringExtra("imageUrl");

        if (imageUri != null) {
            Glide.with(this)
                .load(Uri.parse(imageUri))
                .into(photoView);
        } else if (imageUrl != null) {
            FirebaseStorage.getInstance().getReference(imageUrl)
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Glide.with(this)
                        .load(uri)
                        .into(photoView);
                });
        }

        closeButton.setOnClickListener(v -> finish());
    }
}