package com.example.akherapp;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.akherapp.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.example.akherapp.User;

import java.util.List;

public class LinkedAccountsAdapter extends RecyclerView.Adapter<LinkedAccountsAdapter.ViewHolder> {
    private List<User> accounts;
    private String currentUserId;
    private OnAccountSwitchListener listener;

    public interface OnAccountSwitchListener {
        void onSwitchAccount(String userId);
    }

    public LinkedAccountsAdapter(List<User> accounts, String currentUserId, OnAccountSwitchListener listener) {
        this.accounts = accounts;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_linked_account, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User account = accounts.get(position);
        holder.accountName.setText(account.getFirstName() + " " + account.getLastName());
        holder.accountTeacher.setText(account.getTeacher());
        
        // Charger l'image du profil
        if (account.getProfileImageUrl() != null && !account.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(account.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .into(holder.accountImage);
        }

        // Désactiver le bouton pour le compte actuel
        holder.switchButton.setEnabled(!account.getId().equals(currentUserId));
        holder.switchButton.setOnClickListener(v -> listener.onSwitchAccount(account.getId()));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView accountImage;
        TextView accountName;
        TextView accountTeacher;
        MaterialButton switchButton;

        ViewHolder(View itemView) {
            super(itemView);
            accountImage = itemView.findViewById(R.id.accountImage);
            accountName = itemView.findViewById(R.id.accountName);
            accountTeacher = itemView.findViewById(R.id.accountTeacher);
            switchButton = itemView.findViewById(R.id.switchButton);
        }
    }
}