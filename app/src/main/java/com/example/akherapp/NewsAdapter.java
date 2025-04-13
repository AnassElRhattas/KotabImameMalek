package com.example.akherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private static final String TAG = "NewsAdapter";
    private Context context;
    private List<News> newsList;
    private boolean isAdmin;
    private OnNewsActionListener listener;
    private String currentUserId;

    public interface OnNewsActionListener {
        void onEditClick(News news);
        void onDeleteClick(News news);
    }

    public NewsAdapter(Context context, List<News> newsList, boolean isAdmin, OnNewsActionListener listener) {
        this.context = context;
        this.newsList = newsList;
        this.isAdmin = isAdmin;
        this.listener = listener;
        
        // Obtenir l'ID de l'utilisateur actuel
        SharedPreferences prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        this.currentUserId = prefs.getString("id", "");
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);
        Log.d(TAG, "Binding news item at position " + position + ", isAdmin: " + isAdmin);
        
        holder.titleView.setText(news.getTitle());
        
        // Set up image ViewPager
        NewsImageAdapter imageAdapter = new NewsImageAdapter(context, news.getImageUrls());
        holder.imageViewPager.setAdapter(imageAdapter);
        
        // Set up image indicator if there are multiple images
        if (news.getImageUrls().size() > 1) {
            holder.imageIndicator.setVisibility(View.VISIBLE);
            new TabLayoutMediator(holder.imageIndicator, holder.imageViewPager,
                (tab, position1) -> {
                    // You can customize the tab here if needed
                }).attach();
        } else {
            holder.imageIndicator.setVisibility(View.GONE);
        }

        // Set up description with "Read More" functionality
        String fullDescription = news.getDescription();
        if (fullDescription.length() > 100) {
            holder.descriptionView.setText(fullDescription.substring(0, 100) + "...");
            holder.readMoreButton.setVisibility(View.VISIBLE);
            holder.readMoreButton.setOnClickListener(v -> {
                if (holder.descriptionView.getMaxLines() == 3) {
                    holder.descriptionView.setMaxLines(Integer.MAX_VALUE);
                    holder.descriptionView.setText(fullDescription);
                    holder.readMoreButton.setText("عرض أقل");
                } else {
                    holder.descriptionView.setMaxLines(3);
                    holder.descriptionView.setText(fullDescription.substring(0, 100) + "...");
                    holder.readMoreButton.setText("قراءة المزيد...");
                }
            });
        } else {
            holder.descriptionView.setText(fullDescription);
            holder.readMoreButton.setVisibility(View.GONE);
        }

        // Set timestamp
        if (news.getTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                news.getTimestamp().toDate().getTime(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            );
            holder.timestampView.setText(timeAgo);
        }

        // Afficher/masquer les boutons d'admin
        if (isAdmin) {
            Log.d(TAG, "Showing admin buttons for position " + position);
            holder.adminControls.setVisibility(View.VISIBLE);
            
            holder.btnEdit.setOnClickListener(v -> {
                Log.d(TAG, "Edit button clicked for news: " + news.getId());
                if (listener != null) {
                    listener.onEditClick(news);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                Log.d(TAG, "Delete button clicked for news: " + news.getId());
                if (listener != null) {
                    listener.onDeleteClick(news);
                }
            });
        } else {
            Log.d(TAG, "Hiding admin buttons for position " + position);
            holder.adminControls.setVisibility(View.GONE);
        }

        boolean isLiked = news.getLikes().contains(currentUserId);
        holder.btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.likesCount.setText(String.valueOf(news.getLikesCount()));

        holder.btnLike.setOnClickListener(v -> {
            toggleLike(news, holder);
        });
    }
    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }
public void updateNewsList(List<News> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }
    private void toggleLike(News news, NewsViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<String> likes = new ArrayList<>(news.getLikes());

        if (likes.contains(currentUserId)) {
            likes.remove(currentUserId);
        } else {
            likes.add(currentUserId);
        }

        db.collection("news").document(news.getId())
                .update("likes", likes)
                .addOnSuccessListener(aVoid -> {
                    news.setLikes(likes);
                    boolean isLiked = likes.contains(currentUserId);
                    holder.btnLike.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                    holder.likesCount.setText(String.valueOf(likes.size()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "فشل في تحديث التفاعل", Toast.LENGTH_SHORT).show();
                });
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        ViewPager2 imageViewPager;
        TabLayout imageIndicator;
        TextView descriptionView;
        TextView readMoreButton;
        TextView timestampView;
        LinearLayout adminControls;
        com.google.android.material.button.MaterialButton btnEdit;
        com.google.android.material.button.MaterialButton btnDelete;
        ImageButton btnLike;
        TextView likesCount;

        NewsViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.newsTitle);
            descriptionView = itemView.findViewById(R.id.newsDescription);
            timestampView = itemView.findViewById(R.id.newsTimestamp);
            adminControls = itemView.findViewById(R.id.adminControls);
            btnEdit = itemView.findViewById(R.id.btnEditNews);
            btnDelete = itemView.findViewById(R.id.btnDeleteNews);
            imageViewPager = itemView.findViewById(R.id.imageViewPager);
            imageIndicator = itemView.findViewById(R.id.imageIndicator);
            readMoreButton = itemView.findViewById(R.id.readMoreButton);
            btnLike = itemView.findViewById(R.id.btnLike);
            likesCount = itemView.findViewById(R.id.likesCount);

        }
    }
}