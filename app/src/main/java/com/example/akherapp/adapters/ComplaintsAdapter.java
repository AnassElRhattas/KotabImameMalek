package com.example.akherapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.akherapp.R;
import com.example.akherapp.models.Complaint;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ComplaintsAdapter extends RecyclerView.Adapter<ComplaintsAdapter.ComplaintViewHolder> {
    private List<Complaint> complaints;
    private Context context;
    private OnComplaintActionListener listener;

    public interface OnComplaintActionListener {
        void onRespondClick(Complaint complaint);
        void onMarkAsInProgressClick(Complaint complaint);
        void onMarkAsResolvedClick(Complaint complaint);
        void onDeleteClick(Complaint complaint);
    }

    public ComplaintsAdapter(Context context, List<Complaint> complaints, OnComplaintActionListener listener) {
        this.context = context;
        this.complaints = complaints;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ComplaintViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_complaint, parent, false);
        return new ComplaintViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComplaintViewHolder holder, int position) {
        Complaint complaint = complaints.get(position);

        // Afficher le nom complet de l'utilisateur
        if (holder.userFullNameText != null) {
            String userFullName = complaint.getUserFullName();
            if (userFullName != null && !userFullName.isEmpty()) {
                holder.userFullNameText.setText(userFullName);
                holder.userFullNameText.setVisibility(View.VISIBLE);
            } else {
                holder.userFullNameText.setText("مستخدم غير معروف");
                holder.userFullNameText.setVisibility(View.VISIBLE);
            }
        }

        if (holder.complaintSubject != null && complaint.getSubject() != null) {
            holder.complaintSubject.setText(complaint.getSubject());
        }

        if (holder.complaintDescription != null && complaint.getDescription() != null) {
            holder.complaintDescription.setText(complaint.getDescription());
        }

        if (holder.dateText != null && complaint.getCreatedAt() != null) {
            String formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(complaint.getCreatedAt().toDate());
            holder.dateText.setText(formattedDate);
        }

        setupStatus(holder.statusChip, complaint.getStatus());

        if (complaint.getAdminResponse() != null && !complaint.getAdminResponse().isEmpty()) {
            holder.adminResponseLayout.setVisibility(View.VISIBLE);
            holder.adminResponseText.setText(complaint.getAdminResponse());
        } else {
            holder.adminResponseLayout.setVisibility(View.GONE);
        }

        setupActionButtons(holder, complaint);

        holder.respondButton.setOnClickListener(v -> listener.onRespondClick(complaint));
        holder.inProgressButton.setOnClickListener(v -> listener.onMarkAsInProgressClick(complaint));
        holder.resolvedButton.setOnClickListener(v -> listener.onMarkAsResolvedClick(complaint));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(complaint));
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

    private void setupActionButtons(ComplaintViewHolder holder, Complaint complaint) {
        switch (complaint.getStatus()) {
            case "pending":
                holder.respondButton.setVisibility(View.VISIBLE);
                holder.inProgressButton.setVisibility(View.VISIBLE);
                holder.resolvedButton.setVisibility(View.GONE);
                break;
            case "in_progress":
                holder.respondButton.setVisibility(View.VISIBLE);
                holder.inProgressButton.setVisibility(View.GONE);
                holder.resolvedButton.setVisibility(View.VISIBLE);
                break;
            case "resolved":
                holder.respondButton.setVisibility(View.VISIBLE);
                holder.inProgressButton.setVisibility(View.GONE);
                holder.resolvedButton.setVisibility(View.GONE);
                break;
        }
        holder.deleteButton.setVisibility(View.VISIBLE);
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
        TextView userFullNameText;
        TextView complaintSubject;
        TextView complaintDescription;
        TextView dateText;
        Chip statusChip;
        LinearLayout adminResponseLayout;
        TextView adminResponseText;
        MaterialButton respondButton;
        MaterialButton inProgressButton;
        MaterialButton resolvedButton;
        MaterialButton deleteButton;

        ComplaintViewHolder(View itemView) {
            super(itemView);
            userFullNameText = itemView.findViewById(R.id.userFullNameText);
            complaintSubject = itemView.findViewById(R.id.complaintSubject);
            complaintDescription = itemView.findViewById(R.id.complaintDescription);
            dateText = itemView.findViewById(R.id.dateText);
            statusChip = itemView.findViewById(R.id.statusChip);
            adminResponseLayout = itemView.findViewById(R.id.adminResponseLayout);
            adminResponseText = itemView.findViewById(R.id.adminResponseText);
            respondButton = itemView.findViewById(R.id.respondButton);
            inProgressButton = itemView.findViewById(R.id.inProgressButton);
            resolvedButton = itemView.findViewById(R.id.resolvedButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}