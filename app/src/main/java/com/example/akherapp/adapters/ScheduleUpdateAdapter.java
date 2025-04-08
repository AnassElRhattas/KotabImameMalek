package com.example.akherapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.models.ScheduleUpdate;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ScheduleUpdateAdapter extends RecyclerView.Adapter<ScheduleUpdateAdapter.ViewHolder> {
    private List<ScheduleUpdate> updates;
    private OnScheduleClickListener listener;

    public interface OnScheduleClickListener {
        void onScheduleClick(String scheduleUrl);
    }

    public ScheduleUpdateAdapter(List<ScheduleUpdate> updates, OnScheduleClickListener listener) {
        this.updates = updates;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_update, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ScheduleUpdate update = updates.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.updateDateText.setText(sdf.format(update.getTimestamp()));
        holder.updateDetailsText.setText("تم تحديث الجدول");
        
        holder.viewButton.setOnClickListener(v -> {
            if (listener != null && update.getScheduleUrl() != null) {
                listener.onScheduleClick(update.getScheduleUrl());
            }
        });
    }

    @Override
    public int getItemCount() {
        return updates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView updateDateText;
        TextView updateDetailsText;
        ImageButton viewButton;

        ViewHolder(View itemView) {
            super(itemView);
            updateDateText = itemView.findViewById(R.id.updateDateText);
            updateDetailsText = itemView.findViewById(R.id.updateDetailsText);
            viewButton = itemView.findViewById(R.id.viewButton);
        }
    }
}