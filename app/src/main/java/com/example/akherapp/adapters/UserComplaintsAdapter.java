package com.example.akherapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.models.Complaint;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class UserComplaintsAdapter extends RecyclerView.Adapter<UserComplaintsAdapter.ComplaintViewHolder> {
    private List<Complaint> complaints;
    private Context context;

    public UserComplaintsAdapter(Context context, List<Complaint> complaints) {
        this.context = context;
        this.complaints = complaints;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaints.get(position);
        
        // Définir le sujet
        holder.subjectText.setText(complaint.getSubject());
        
        // Définir la description
        holder.descriptionText.setText(complaint.getDescription());
        
        // Formater et définir la date
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String date = sdf.format(complaint.getCreatedAt().toDate());
        holder.dateText.setText(date);
        
        // Configurer le statut
        setupStatus(holder.statusChip, complaint.getStatus());
        
        // Gérer la réponse de l'admin
        if (complaint.getAdminResponse() != null && !complaint.getAdminResponse().isEmpty()) {
            holder.adminResponseLayout.setVisibility(View.VISIBLE);
            holder.adminResponseText.setText(complaint.getAdminResponse());
        } else {
            holder.adminResponseLayout.setVisibility(View.GONE);
        }
    }

    private void setupStatus(Chip chip, String status) {
        int backgroundColor;
        String statusText;
        
        switch (status) {
            case "pending":
                backgroundColor = R.color.status_pending;
                statusText = "في انتظار المراجعة";
                break;
            case "in_progress":
                backgroundColor = R.color.status_in_progress;
                statusText = "قيد المعالجة";
                break;
            case "resolved":
                backgroundColor = R.color.status_resolved;
                statusText = "تم الحل";
                break;
            default:
                backgroundColor = R.color.status_pending;
                statusText = "في انتظار المراجعة";
        }
        
        chip.setChipBackgroundColorResource(backgroundColor);
        chip.setText(statusText);
    }

    @Override
    public int getItemCount() {
        return complaints != null ? complaints.size() : 0;
    }

    public void updateComplaints(List<Complaint> newComplaints) {
        this.complaints = newComplaints;
        notifyDataSetChanged();
    }

    static class ComplaintViewHolder extends RecyclerView.ViewHolder {
        TextView subjectText;
        TextView dateText;
        TextView descriptionText;
        Chip statusChip;
        LinearLayout adminResponseLayout;
        TextView adminResponseText;

        ComplaintViewHolder(View itemView) {
            super(itemView);
            subjectText = itemView.findViewById(R.id.subjectText);
            dateText = itemView.findViewById(R.id.dateText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            statusChip = itemView.findViewById(R.id.statusChip);
            adminResponseLayout = itemView.findViewById(R.id.adminResponseLayout);
            adminResponseText = itemView.findViewById(R.id.adminResponseText);
        }
    }
} 