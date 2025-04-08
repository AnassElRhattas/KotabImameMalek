package com.example.akherapp.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.akherapp.R;
import com.example.akherapp.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDocumentsAdapter extends RecyclerView.Adapter<UserDocumentsAdapter.ViewHolder> {
    private Context context;
    private List<User> users;
    private OnDocumentActionListener listener;

    public interface OnDocumentActionListener {
        void onViewDocument(String documentUrl);
        void onApproveDocument(User user, String documentType);
        void onRejectDocument(User user, String documentType);
    }

    public UserDocumentsAdapter(Context context, List<User> users, OnDocumentActionListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_documents, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        
        // Set user info
        holder.userName.setText(user.getFirstName() + " " + user.getLastName());
        
        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.default_profile_image)
                .error(R.drawable.default_profile_image)
                .circleCrop()
                .into(holder.userProfileImage);
        } else {
            holder.userProfileImage.setImageResource(R.drawable.default_profile_image);
        }

        // Get documents map
        Map<String, Object> documents = user.getDocuments();
        
        // Check documents status
        boolean allDocumentsSubmitted = true;
        boolean allDocumentsVerified = true;
        
        if (documents != null) {
            // Check ID Card
            String idCard = (String) documents.get("id_card");
            Boolean idCardVerified = (Boolean) documents.get("id_card_verified");
            if (idCard == null || idCardVerified == null || !idCardVerified) {
                allDocumentsVerified = false;
            }
            if (idCard == null) allDocumentsSubmitted = false;
            
            // Check Photo
            String photo = (String) documents.get("photo");
            Boolean photoVerified = (Boolean) documents.get("photo_verified");
            if (photo == null || photoVerified == null || !photoVerified) {
                allDocumentsVerified = false;
            }
            if (photo == null) allDocumentsSubmitted = false;
            
            // Check Certificate
            String certificate = (String) documents.get("school_certificate");
            Boolean certificateVerified = (Boolean) documents.get("school_certificate_verified");
            if (certificate == null || certificateVerified == null || !certificateVerified) {
                allDocumentsVerified = false;
            }
            if (certificate == null) allDocumentsSubmitted = false;

            // Setup document sections
            setupDocumentSection(
                holder.viewIdCardButton, 
                holder.approveIdCardButton, 
                holder.rejectIdCardButton, 
                idCard, 
                user, 
                "id_card",
                holder.idCardActionsContainer
            );
            
            setupDocumentSection(
                holder.viewPhotoButton, 
                holder.approvePhotoButton, 
                holder.rejectPhotoButton, 
                photo, 
                user, 
                "photo",
                holder.photoActionsContainer
            );
            
            setupDocumentSection(
                holder.viewCertificateButton, 
                holder.approveCertificateButton, 
                holder.rejectCertificateButton, 
                certificate, 
                user, 
                "school_certificate",
                holder.certificateActionsContainer
            );
        } else {
            allDocumentsSubmitted = false;
            allDocumentsVerified = false;
        }

        // Show registration status
        // In onBindViewHolder method, replace the registration status section with:
        holder.registrationStatus.setVisibility(View.VISIBLE);
        if (!allDocumentsSubmitted) {
            holder.registrationStatus.setText("❌ لم يتم إكمال تقديم المستندات");
            holder.registrationStatus.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.red_light)));
            holder.registrationStatus.setTextColor(context.getColor(R.color.red));
        } else if (!allDocumentsVerified) {
            holder.registrationStatus.setText("⏳ في انتظار التحقق من المستندات");
            holder.registrationStatus.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.orange_light)));
            holder.registrationStatus.setTextColor(context.getColor(R.color.orange));
        } else {
            holder.registrationStatus.setText("✅ تم إكمال التسجيل");
            holder.registrationStatus.setBackgroundTintList(ColorStateList.valueOf(context.getColor(R.color.green_light)));
            holder.registrationStatus.setTextColor(context.getColor(R.color.green));
            if (!user.isRegistrationCompletionNotified()) {
                sendRegistrationCompletionNotification(user);
            }
        }
    }

    private void setupDocumentSection(MaterialButton viewButton, MaterialButton approveButton, 
            MaterialButton rejectButton, String documentUrl, User user, String documentType, View actionsContainer) {
        if (documentUrl != null && !documentUrl.isEmpty()) {
            viewButton.setEnabled(true);
            actionsContainer.setVisibility(View.VISIBLE);
            viewButton.setOnClickListener(v -> listener.onViewDocument(documentUrl));
            
            approveButton.setOnClickListener(v -> {
                listener.onApproveDocument(user, documentType);
                updateDocumentButtonState(approveButton, rejectButton, true, false);
            });
            
            rejectButton.setOnClickListener(v -> {
                listener.onRejectDocument(user, documentType);
                updateDocumentButtonState(approveButton, rejectButton, false, true);
            });
            
            boolean isVerified = user.isDocumentVerified(documentType);
            boolean isRejected = user.isDocumentRejected(documentType);
            updateDocumentButtonState(approveButton, rejectButton, isVerified, isRejected);
        } else {
            viewButton.setEnabled(false);
            actionsContainer.setVisibility(View.GONE);
        }
    }

    private void updateDocumentButtonState(MaterialButton approveButton, MaterialButton rejectButton, 
            boolean isVerified, boolean isRejected) {
        if (isVerified) {
            approveButton.setEnabled(false);
            approveButton.setText("تمت الموافقة");
            rejectButton.setEnabled(true);
            rejectButton.setText("رفض");
        } else if (isRejected) {
            approveButton.setEnabled(true);
            approveButton.setText("موافقة");
            rejectButton.setEnabled(false);
            rejectButton.setText("تم الرفض");
        } else {
            approveButton.setEnabled(true);
            approveButton.setText("موافقة");
            rejectButton.setEnabled(true);
            rejectButton.setText("رفض");
        }
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView userProfileImage;
        TextView userName;
        MaterialButton viewIdCardButton, viewPhotoButton, viewCertificateButton;
        MaterialButton approveIdCardButton, approvePhotoButton, approveCertificateButton;
        MaterialButton rejectIdCardButton, rejectPhotoButton, rejectCertificateButton;
        View idCardActionsContainer;
        View photoActionsContainer;
        View certificateActionsContainer;
        TextView registrationStatus;

        ViewHolder(View itemView) {
            super(itemView);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            userName = itemView.findViewById(R.id.userName);
            registrationStatus = itemView.findViewById(R.id.registrationStatus);
            
            viewIdCardButton = itemView.findViewById(R.id.viewIdCardButton);
            viewPhotoButton = itemView.findViewById(R.id.viewPhotoButton);
            viewCertificateButton = itemView.findViewById(R.id.viewCertificateButton);
            
            approveIdCardButton = itemView.findViewById(R.id.approveIdCardButton);
            approvePhotoButton = itemView.findViewById(R.id.approvePhotoButton);
            approveCertificateButton = itemView.findViewById(R.id.approveCertificateButton);
            
            rejectIdCardButton = itemView.findViewById(R.id.rejectIdCardButton);
            rejectPhotoButton = itemView.findViewById(R.id.rejectPhotoButton);
            rejectCertificateButton = itemView.findViewById(R.id.rejectCertificateButton);

            // Initialize action containers
            idCardActionsContainer = itemView.findViewById(R.id.idCardActionsContainer);
            photoActionsContainer = itemView.findViewById(R.id.photoActionsContainer);
            certificateActionsContainer = itemView.findViewById(R.id.certificateActionsContainer);
        }
    }

    private void sendRegistrationCompletionNotification(User user) {
        // Update user's notification status in Firestore
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.getId())
            .update(
                "registrationCompletionNotified", true,
                "registrationCompleted", true
            )
            .addOnSuccessListener(aVoid -> {
                // Send FCM notification
                Map<String, Object> notification = new HashMap<>();
                notification.put("userId", user.getId());
                notification.put("title", "تهانينا!");
                notification.put("body", "تم التحقق من جميع مستنداتك بنجاح. اكتمل تسجيلك!");
                notification.put("type", "DOCUMENTS_VERIFIED");

                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .add(notification);
            });
    }
}