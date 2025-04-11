package com.example.akherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.akherapp.utils.NotificationHelper;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public abstract class BaseUserActivity extends AppCompatActivity {
    protected FirebaseFirestore db;
    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem switchAccountItem = menu.findItem(R.id.action_switch_account);
        
        // Vérifier si l'utilisateur a des comptes liés
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean hasLinkedAccounts = prefs.getBoolean("has_linked_accounts", false);
        switchAccountItem.setVisible(hasLinkedAccounts);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_switch_account) {
            // Votre logique pour l'action de changement de compte
            showLinkedAccounts();
            return true;
        } else if (itemId == R.id.action_notifications) {
            // Récupérer l'ID de l'utilisateur depuis les SharedPreferences
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String userId = prefs.getString("id", null);

            // Vérifiez si l'ID de l'utilisateur existe
            if (userId != null) {
                // Si vous avez un objet 'user' existant, utilisez son ID.
                NotificationHelper.showNotificationsDialog(this, userId);  // Utilisation de userId récupéré
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    protected void showLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "خطأ في تحميل الحسابات المرتبطة", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                View headerView = navigationView.getHeaderView(0);
                TextView linkedAccountsTitle = headerView.findViewById(R.id.linkedAccountsTitle);
                LinearLayout linkedAccountsContainer = headerView.findViewById(R.id.linkedAccountsContainer);

                linkedAccountsTitle.setVisibility(View.VISIBLE);
                linkedAccountsContainer.removeAllViews();

                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    String userId = document.getId();
                    
                    if (!userId.equals(currentUserId)) {
                        View accountView = getLayoutInflater().inflate(R.layout.layout_linked_account, linkedAccountsContainer, false);
                        
                        TextView nameText = accountView.findViewById(R.id.linkedAccountName);
                        MaterialButton switchButton = accountView.findViewById(R.id.btnSwitchAccount);

                        nameText.setText(user.getFirstName() + " " + user.getLastName());
                        
                        switchButton.setOnClickListener(v -> switchToAccount(userId));
                        linkedAccountsContainer.addView(accountView);
                    }
                }
                
                drawerLayout.openDrawer(GravityCompat.START);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "خطأ في تحميل الحسابات المرتبطة", Toast.LENGTH_SHORT).show();
            });
    }

    protected void switchToAccount(String userId) {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putString("id", userId).apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    protected void checkLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (phone == null || phone.isEmpty()) {
            return;
        }

        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean foundLinkedAccounts = false;
                for (DocumentSnapshot doc : queryDocumentSnapshots) {
                    if (!doc.getId().equals(currentUserId)) {
                        foundLinkedAccounts = true;
                        break;
                    }
                }
                
                prefs.edit().putBoolean("has_linked_accounts", foundLinkedAccounts).apply();
                invalidateOptionsMenu(); // Mettre à jour le menu
            });
    }


}
