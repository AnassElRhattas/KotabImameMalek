package com.example.akherapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UserMessageAdapter extends RecyclerView.Adapter<UserMessageAdapter.UserViewHolder> {
    private List<String> userIds;
    private OnUserClickListener listener;
    private FirebaseFirestore db;

    public interface OnUserClickListener {
        void onUserClick(String userId);
    }

    public UserMessageAdapter(List<String> userIds, OnUserClickListener listener) {
        this.userIds = userIds;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String userId = userIds.get(position);
        
        // Fetch user details from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    String fullName = (firstName != null ? firstName : "") + " " + 
                                    (lastName != null ? lastName : "");
                    holder.nameTextView.setText(fullName.trim());
                }
            });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(userId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userIds.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;

        UserViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.adminNameText);
        }
    }
}