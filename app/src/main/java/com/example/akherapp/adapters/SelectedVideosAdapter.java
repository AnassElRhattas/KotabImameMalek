package com.example.akherapp.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import java.util.ArrayList;
import java.util.List;

public class SelectedVideosAdapter extends RecyclerView.Adapter<SelectedVideosAdapter.VideoViewHolder> {
    private List<Uri> videoUris;
    private OnVideoRemoveClickListener removeListener;

    public interface OnVideoRemoveClickListener {
        void onVideoRemove(int position);
    }

    public SelectedVideosAdapter(OnVideoRemoveClickListener listener) {
        this.videoUris = new ArrayList<>();
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selected_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Uri videoUri = videoUris.get(position);
        // Set video thumbnail
        holder.videoThumbnail.setImageResource(R.drawable.ic_video_thumbnail);
        
        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onVideoRemove(position);
            }
        });
    }



    @Override
    public int getItemCount() {
        return videoUris.size();
    }

    public void addVideo(Uri videoUri) {
        videoUris.add(videoUri);
        notifyItemInserted(videoUris.size() - 1);
    }

    public void removeVideo(int position) {
        if (position >= 0 && position < videoUris.size()) {
            videoUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, videoUris.size());
        }
    }

    public void clearVideos() {
        videoUris.clear();
        notifyDataSetChanged();
    }

    public List<Uri> getVideoUris() {
        return new ArrayList<>(videoUris);
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView videoThumbnail;
        ImageButton removeButton;

        VideoViewHolder(View itemView) {
            super(itemView);
            videoThumbnail = itemView.findViewById(R.id.videoThumbnail);
            removeButton = itemView.findViewById(R.id.btnRemoveVideo);
        }
    }
}