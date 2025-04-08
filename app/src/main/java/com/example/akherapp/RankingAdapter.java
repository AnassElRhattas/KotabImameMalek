package com.example.akherapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {
    private List<UserRanking> rankings;

    public RankingAdapter(List<UserRanking> rankings) {
        this.rankings = rankings;
    }

    @NonNull
    @Override
    public RankingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ranking, parent, false);
        return new RankingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
        UserRanking ranking = rankings.get(position);
        User user = ranking.getUser();
        
        // Set rank number (position + 1 because position starts at 0)
        holder.rankText.setText(String.valueOf(position + 1));
        
        // Set user name
        String fullName = user.getFirstName() + " " + user.getLastName();
        holder.nameText.setText(fullName);
        
        // Set progress
        holder.progressText.setText(String.format("%.0f آية", ranking.getTotalProgress()));
    }

    @Override
    public int getItemCount() {
        return rankings.size();
    }

    static class RankingViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        TextView nameText;
        TextView progressText;

        RankingViewHolder(View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            nameText = itemView.findViewById(R.id.nameText);
            progressText = itemView.findViewById(R.id.progressText);
        }
    }
}