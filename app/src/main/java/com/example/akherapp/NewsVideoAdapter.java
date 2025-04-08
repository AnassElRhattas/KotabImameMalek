package com.example.akherapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class NewsVideoAdapter extends RecyclerView.Adapter<NewsVideoAdapter.VideoViewHolder> {
    private Context context;
    private List<String> videoUrls;

    public NewsVideoAdapter(Context context, List<String> videoUrls) {
        this.context = context;
        this.videoUrls = videoUrls;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_news_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        String videoUrl = videoUrls.get(position);
        
        // Set video thumbnail
        Glide.with(context)
            .load(videoUrl)
            .placeholder(R.drawable.ic_video_thumbnail)
            .into(holder.thumbnailView);

        // Handle click to play video
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoUrl), "video/*");
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return videoUrls != null ? videoUrls.size() : 0;
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailView;
        ImageView playButton;

        VideoViewHolder(View itemView) {
            super(itemView);
            thumbnailView = itemView.findViewById(R.id.videoThumbnail);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }
}