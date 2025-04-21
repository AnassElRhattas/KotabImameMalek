package com.example.akherapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.akherapp.R;
import com.example.akherapp.models.VideoCall;
import java.util.List;

public class VideoCallAdapter extends RecyclerView.Adapter<VideoCallAdapter.CallViewHolder> {
    private List<VideoCall> calls;
    private CallActionListener listener;

    public interface CallActionListener {
        void onCallAction(VideoCall call, boolean accept);
    }

    public VideoCallAdapter(List<VideoCall> calls, CallActionListener listener) {
        this.calls = calls;
        this.listener = listener;
    }

    @Override
    public CallViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_call, parent, false);
        return new CallViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CallViewHolder holder, int position) {
        VideoCall call = calls.get(position);
        holder.callerIdText.setText("المتصل: " + call.getCallerId());
        holder.timestampText.setText(call.getTimestamp().toDate().toString());
        
        holder.acceptButton.setOnClickListener(v -> 
            listener.onCallAction(call, true));
        holder.rejectButton.setOnClickListener(v -> 
            listener.onCallAction(call, false));
    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

    static class CallViewHolder extends RecyclerView.ViewHolder {
        TextView callerIdText;
        TextView timestampText;
        Button acceptButton;
        Button rejectButton;

        CallViewHolder(View itemView) {
            super(itemView);
            callerIdText = itemView.findViewById(R.id.callerIdText);
            timestampText = itemView.findViewById(R.id.timestampText);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
        }
    }
}