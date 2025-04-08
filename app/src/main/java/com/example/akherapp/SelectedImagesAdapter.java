package com.example.akherapp;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ImageViewHolder> {
    private List<Uri> imageUris = new ArrayList<>();
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onDeleteClick(int position);
    }

    public SelectedImagesAdapter(OnImageClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        holder.bind(imageUri, position);
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public void addImage(Uri uri) {
        imageUris.add(uri);
        notifyItemInserted(imageUris.size() - 1);
    }

    public void removeImage(int position) {
        imageUris.remove(position);
        notifyItemRemoved(position);
    }

    public List<Uri> getImageUris() {
        return imageUris;
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private ImageButton btnDelete;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Uri imageUri, int position) {
            Glide.with(itemView.getContext())
                    .load(imageUri)
                    .into(imageView);
            
            btnDelete.setOnClickListener(v -> listener.onDeleteClick(position));
        }
    }
} 