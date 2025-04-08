package com.example.akherapp;

import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {
    private static final String TAG = "NewsAdapter";
    private Context context;
    private List<News> newsList;
    private boolean isAdmin;
    private OnNewsActionListener listener;

    public interface OnNewsActionListener {
        void onEditClick(News news);
        void onDeleteClick(News news);
    }

    public NewsAdapter(Context context, List<News> newsList, boolean isAdmin, OnNewsActionListener listener) {
        this.context = context;
        this.newsList = newsList;
        this.isAdmin = isAdmin;
        this.listener = listener;
        Log.d(TAG, "NewsAdapter created with isAdmin: " + isAdmin);
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

        holder.titleView.setText(news.getTitle());

        // Handle images
        if (!news.getImageUrls().isEmpty()) {
            holder.imageContainer.setVisibility(View.VISIBLE);
            NewsImageAdapter imageAdapter = new NewsImageAdapter(context, news.getImageUrls());
            holder.imageViewPager.setAdapter(imageAdapter);

            if (news.getImageUrls().size() > 1) {
                holder.imageIndicator.setVisibility(View.VISIBLE);
                new TabLayoutMediator(holder.imageIndicator, holder.imageViewPager,
                        (tab, position1) -> {}).attach();
            } else {
                holder.imageIndicator.setVisibility(View.GONE);
            }
        } else {
            holder.imageContainer.setVisibility(View.GONE);
        }

        // Handle videos
        if (!news.getVideoUrls().isEmpty()) {
            holder.videoRecyclerView.setVisibility(View.VISIBLE);
            NewsVideoAdapter videoAdapter = new NewsVideoAdapter(context, news.getVideoUrls());
            holder.videoRecyclerView.setAdapter(videoAdapter);
        } else {
            holder.videoRecyclerView.setVisibility(View.GONE);
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
    }

    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }

    public void updateNewsList(List<News> newsList) {
        this.newsList = newsList;
        notifyDataSetChanged();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ViewPager2 imageViewPager;
        TabLayout imageIndicator;
        RecyclerView videoRecyclerView;
        TextView titleView;
        TextView descriptionView;
        TextView timestampView;
        TextView readMoreButton;
        LinearLayout adminControls;
        com.google.android.material.button.MaterialButton btnEdit;
        com.google.android.material.button.MaterialButton btnDelete;
        View imageContainer;

        NewsViewHolder(View itemView) {
            super(itemView);
            imageViewPager = itemView.findViewById(R.id.imageViewPager);
            imageIndicator = itemView.findViewById(R.id.imageIndicator);
            videoRecyclerView = itemView.findViewById(R.id.videoRecyclerView);
            titleView = itemView.findViewById(R.id.newsTitle);
            descriptionView = itemView.findViewById(R.id.newsDescription);
            timestampView = itemView.findViewById(R.id.newsTimestamp);
            readMoreButton = itemView.findViewById(R.id.readMoreButton);
            adminControls = itemView.findViewById(R.id.adminControls);
            btnEdit = itemView.findViewById(R.id.btnEditNews);
            btnDelete = itemView.findViewById(R.id.btnDeleteNews);
            imageContainer = itemView.findViewById(R.id.imageContainer);
        }
    }
}