package com.example.akherapp;

import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.AchievementViewHolder> {
    private List<Achievement> achievements;

    public AchievementsAdapter(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);

        // Always show the badge with empty state
        holder.badgeImage.setImageResource(R.drawable.badge_empty);

        // Only show progress if there is any
        if (achievement.getProgress() > 0) {
            LayerDrawable layerDrawable = (LayerDrawable) holder.badgeImage.getContext()
                    .getResources().getDrawable(achievement.getBadgeResourceId());
            ClipDrawable clipDrawable = new ClipDrawable(layerDrawable,
                    android.view.Gravity.BOTTOM, ClipDrawable.VERTICAL);

            holder.badgeImage.setImageDrawable(clipDrawable);

            int level = (int) (achievement.getProgressPercentage() * 10000);
            clipDrawable.setLevel(level);
        }

        // Show progress in title even if it's 0
        holder.badgeTitle.setText(String.format("%s (%.0f/%.0f)",
                achievement.getTitle(),
                achievement.getProgress(),
                achievement.getRequiredProgress()));
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    static class AchievementViewHolder extends RecyclerView.ViewHolder {
        ImageView badgeImage;
        TextView badgeTitle;

        AchievementViewHolder(View itemView) {
            super(itemView);
            badgeImage = itemView.findViewById(R.id.badgeImage);
            badgeTitle = itemView.findViewById(R.id.badgeTitle);
        }
    }
}