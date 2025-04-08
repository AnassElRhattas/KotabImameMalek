package com.example.akherapp;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.akherapp.utils.DateUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Context;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.ArrayList;
import java.util.List;

public class UsersListAdapter extends RecyclerView.Adapter<UsersListAdapter.UserViewHolder> {
    private List<User> users;
    private List<User> usersFiltered;
    private FirebaseFirestore db;
    private Context context;  // Add this field

    public UsersListAdapter(List<User> users) {
        this.users = users;
        this.usersFiltered = new ArrayList<>(users);
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();  // Initialize context here
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = usersFiltered.get(position);

        // Afficher le nom complet
        String fullName = user.getFirstName() + " " + user.getLastName();
        holder.userNameText.setText(fullName);

        // Afficher le nom du maître
        if (user.getTeacher() != null && !user.getTeacher().isEmpty()) {
            holder.userTeacherText.setText("المعلم: " + user.getTeacher());
            holder.userTeacherText.setVisibility(View.VISIBLE);
        } else {
            holder.userTeacherText.setVisibility(View.GONE);
        }

        // Afficher le numéro de téléphone
        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            holder.userPhoneText.setText(user.getPhone());
            holder.userPhoneText.setVisibility(View.VISIBLE);
        } else {
            holder.userPhoneText.setVisibility(View.GONE);
        }

        // Calculate and display age if birth date is available
        String birthDateStr = user.getBirthDate();
        if (birthDateStr != null && !birthDateStr.isEmpty()) {
            int age = DateUtils.calculateAge(birthDateStr);
            if (age >= 0) {
                holder.userAgeText.setText("العمر: " + age + " سنة");
                holder.userAgeText.setVisibility(View.VISIBLE);
            } else {
                holder.userAgeText.setVisibility(View.GONE);
            }
        } else {
            holder.userAgeText.setVisibility(View.GONE);
        }

        // Charger l'image de profil
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.userImageView);
        } else {
            holder.userImageView.setImageResource(R.drawable.default_profile);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), UserDetailsActivity.class);
            intent.putExtra("USER_ID", user.getId());
            v.getContext().startActivity(intent);
        });

        holder.deleteUserButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(v.getContext())
                    .setTitle("تأكيد الحذف")
                    .setMessage("هل أنت متأكد من حذف هذا المستخدم؟")
                    .setPositiveButton("حذف", (dialog, which) -> deleteUser(user, position))
                    .setNegativeButton("إلغاء", null)
                    .show();
        });
    }

    private void deleteUser(User user, int position) {
        db.collection("users").document(user.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    users.remove(user);
                    usersFiltered.remove(user);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, usersFiltered.size());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "فشل في حذف المستخدم", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return usersFiltered != null ? usersFiltered.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userNameText;
        TextView userTeacherText;
        TextView userPhoneText;
        TextView userAgeText;
        CircleImageView userImageView;
        ImageButton deleteUserButton;

        UserViewHolder(View itemView) {
            super(itemView);
            userNameText = itemView.findViewById(R.id.userNameText);
            userTeacherText = itemView.findViewById(R.id.userTeacherText);
            userPhoneText = itemView.findViewById(R.id.userPhoneText);
            userAgeText = itemView.findViewById(R.id.userAgeText);
            userImageView = itemView.findViewById(R.id.userImageView);
            deleteUserButton = itemView.findViewById(R.id.deleteUserButton); // Add this line
        }
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        this.usersFiltered = new ArrayList<>(newUsers);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        usersFiltered.clear();
        if (query.isEmpty()) {
            usersFiltered.addAll(users);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : users) {
                String fullName = (user.getFirstName() + " " + user.getLastName()).toLowerCase();
                if (fullName.contains(lowerCaseQuery) ||
                        (user.getPhone() != null && user.getPhone().contains(query)) ||
                        (user.getTeacher() != null && user.getTeacher().toLowerCase().contains(lowerCaseQuery))) {
                    usersFiltered.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }
}
