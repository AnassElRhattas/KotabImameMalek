package com.example.akherapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.User;
import java.util.ArrayList;
import java.util.List;

public class UserContactAdapter extends RecyclerView.Adapter<UserContactAdapter.UserViewHolder> {
    private List<User> users;
    private List<User> filteredUsers;
    private Context context;

    public UserContactAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
        this.filteredUsers = new ArrayList<>(users);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_contact, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = filteredUsers.get(position);
        
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                         (user.getLastName() != null ? user.getLastName() : "");
        holder.userName.setText(fullName.trim());
        holder.userPhone.setText(user.getPhone());

        holder.whatsappButton.setOnClickListener(v -> openWhatsApp(user.getPhone()));
    }

    @Override
    public int getItemCount() {
        return filteredUsers != null ? filteredUsers.size() : 0;
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            String formattedNumber = phoneNumber;
            if (!formattedNumber.startsWith("+")) {
                formattedNumber = "+212" + formattedNumber;
            }
            
            String url = "https://api.whatsapp.com/send?phone=" + formattedNumber.replace("+", "");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "تعذر فتح WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        this.filteredUsers = new ArrayList<>(newUsers);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(users);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : users) {
                String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                (user.getLastName() != null ? user.getLastName() : "")).toLowerCase();
                if (fullName.contains(lowerCaseQuery) || 
                    (user.getPhone() != null && user.getPhone().contains(query))) {
                    filteredUsers.add(user);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView userPhone;
        ImageView whatsappButton;

        UserViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userPhone = itemView.findViewById(R.id.userPhone);
            whatsappButton = itemView.findViewById(R.id.whatsappButton);
        }
    }
}
