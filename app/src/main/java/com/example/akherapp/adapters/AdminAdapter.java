package com.example.akherapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.User;
import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {
    private List<User> adminList;
    private OnAdminClickListener listener;

    public interface OnAdminClickListener {
        void onAdminClick(User admin);
    }

    public AdminAdapter(List<User> adminList, OnAdminClickListener listener) {
        this.adminList = adminList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        User admin = adminList.get(position);
        String fullName = admin.getFirstName() + " " + admin.getLastName();
        holder.nameTextView.setText(fullName);
        
        // Set online status
        String statusText = admin.isOnline() ? "متصل" : "غير متصل";
        holder.statusTextView.setText(statusText);
        holder.statusTextView.setTextColor(holder.itemView.getContext().getColor(
            admin.isOnline() ? R.color.green : R.color.red
        ));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAdminClick(admin);
            }
        });
    }

    @Override
    public int getItemCount() {
        return adminList.size();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView statusTextView;

        AdminViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.adminNameText);
            statusTextView = itemView.findViewById(R.id.adminStatusText);
        }
    }
}