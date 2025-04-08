package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.app.ActivityCompat.invalidateOptionsMenu;
import static androidx.core.content.ContextCompat.startActivity;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseUserActivity {
    private RecyclerView newsRecyclerView;
    private NewsAdapter newsAdapter;
    private BroadcastReceiver notificationReceiver;
    private boolean isAdmin;
    private Toolbar toolbar;
    private FloatingActionButton fab;

    // Constante pour l'action de notification
    public static final String NOTIFICATION_RECEIVED = "com.example.akherapp.NOTIFICATION_RECEIVED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = FirebaseFirestore.getInstance();

        // Initialisation des vues
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        newsRecyclerView = findViewById(R.id.newsRecyclerView);
        fab = findViewById(R.id.fab);

        // Configuration du RecyclerView
        newsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Vérifier le rôle admin
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        isAdmin = "admin".equals(role);
        Log.d("MainActivity", "Role: " + role + ", isAdmin: " + isAdmin);

        // Créer l'adaptateur avec le bon état admin
        newsAdapter = new NewsAdapter(this, new ArrayList<>(), isAdmin, new NewsAdapter.OnNewsActionListener() {
            @Override
            public void onEditClick(News news) {
                Log.d("MainActivity", "Edit clicked for news: " + news.getId());
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                intent.putExtra("newsId", news.getId());
                intent.putExtra("isEditing", true);
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(News news) {
                Log.d("MainActivity", "Delete clicked for news: " + news.getId());
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("تأكيد الحذف")
                        .setMessage("هل أنت متأكد من حذف هذا الخبر؟")
                        .setPositiveButton("نعم", (dialog, which) -> deleteNews(news))
                        .setNegativeButton("لا", null)
                        .show();
            }
        });
        newsRecyclerView.setAdapter(newsAdapter);

        // Configurer le FAB en fonction du rôle
        if (isAdmin) {
            fab.setVisibility(View.VISIBLE);
            fab.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
            });
        } else {
            fab.setVisibility(View.GONE);
        }

        // Configurer les autres composants
        setupNavigationDrawer();
        setupNotificationReceiver();
        loadNews();
        checkLinkedAccounts();
        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_home);
    }

    private void setupNavigationDrawer() {
        // Configurer le menu et l'en-tête selon le rôle de l'utilisateur
        navigationView.getMenu().clear();
        if (isAdmin) {
            navigationView.inflateMenu(R.menu.admin_menu);
            navigationView.inflateHeaderView(R.layout.nav_header_admin);
        } else {
            navigationView.inflateMenu(R.menu.user_menu);
            navigationView.inflateHeaderView(R.layout.nav_header_user);
        }

        // Configuration du toggle pour ouvrir/fermer le menu
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Charger les informations de l'utilisateur dans l'en-tête
        View headerView = navigationView.getHeaderView(0);
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                TextView nameView = headerView.findViewById(R.id.nav_header_name);
                                ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

                                String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                        (user.getLastName() != null ? user.getLastName() : "");

                                // Configurer le nom selon le rôle
                                if (nameView != null) {
                                    nameView.setText(fullName.trim());
                                }

                                if (isAdmin) {
                                    TextView roleView = headerView.findViewById(R.id.nav_header_role);
                                    if (roleView != null) {
                                        roleView.setText("مدير النظام");
                                    }
                                } else {
                                    TextView phoneView = headerView.findViewById(R.id.nav_header_phone);
                                    if (phoneView != null) {
                                        phoneView.setText(user.getPhone() != null ? user.getPhone() : "");
                                    }
                                }

                                // Charger l'image de profil
                                if (profileImageView != null) {
                                    String imageUrl = user.getProfileImageUrl();
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        Glide.with(this)
                                                .load(imageUrl)
                                                .placeholder(R.drawable.default_profile_image)
                                                .error(R.drawable.default_profile_image)
                                                .circleCrop()
                                                .into(profileImageView);
                                    } else {
                                        profileImageView.setImageResource(R.drawable.default_profile_image);
                                    }
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this,
                                "خطأ في تحميل معلومات المستخدم",
                                Toast.LENGTH_SHORT).show();
                    });
        }

        // Gérer les clics sur les éléments du menu
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_add_news && isAdmin) {
                startActivity(new Intent(this, AdminActivity.class));
            } else if (id == R.id.menu_verify_documents && isAdmin) {
                startActivity(new Intent(this, VerifyDocumentsActivity.class));
            } else if (id == R.id.menu_schedule && isAdmin) {
                startActivity(new Intent(this, ScheduleManagementActivity.class));
            } else if (id == R.id.menu_users && isAdmin) {
                startActivity(new Intent(this, UsersListActivity.class));
            } else if (id == R.id.menu_contact_users && isAdmin) {
                startActivity(new Intent(this, ContactUsersActivity.class));
            } else if (id == R.id.menu_documents && !isAdmin) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_complaints && isAdmin) {
                startActivity(new Intent(this, ManageComplaintsActivity.class));
            } else if (id == R.id.menu_progress && !isAdmin) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
            } else if (id == R.id.menu_schedule && !isAdmin) {
                startActivity(new Intent(this, ViewScheduleActivity.class));
            } else if (id == R.id.menu_profile) {
                Intent intent;
                if (isAdmin) {
                    intent = new Intent(this, AdminProfileActivity.class);
                } else {
                    intent = new Intent(this, UserProfileActivity.class);
                }
                startActivity(intent);
            } else if (id == R.id.menu_logout) {
                // Déconnexion
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else if (id == R.id.menu_submit_complaint) {
                startActivity(new Intent(this, SubmitComplaintActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void setupNotificationReceiver() {
        Log.d("MainActivity", "Configuration du receiver de notifications");
        // Initialiser le receiver pour les notifications en premier plan
        notificationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String title = intent.getStringExtra("title");
                String message = intent.getStringExtra("message");
                showInAppNotification(title, message);
            }
        };

        // Enregistrer le receiver
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(notificationReceiver, new IntentFilter(NOTIFICATION_RECEIVED));
    }

    private void showInAppNotification(String title, String message) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
    }

    private void loadNews() {
        Log.d("MainActivity", "Loading news, isAdmin: " + isAdmin);
        db.collection("news")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<News> newsList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        News news = document.toObject(News.class);
                        news.setId(document.getId());
                        newsList.add(news);
                    }
                    newsAdapter.updateNewsList(newsList);
                    Log.d("MainActivity", "Loaded " + newsList.size() + " news items");
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading news", e);
                    Toast.makeText(MainActivity.this, "خطأ في تحميل الأخبار", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteNews(News news) {
        if (!isAdmin) {
            Log.w("MainActivity", "Tentative de suppression sans droits admin");
            return;
        }

        // First delete images from Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        List<String> imageUrls = news.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    // Get the storage reference from the URL
                    StorageReference imageRef = storage.getReferenceFromUrl(imageUrl);
                    
                    // Delete the image
                    imageRef.delete()
                        .addOnFailureListener(e -> {
                            Log.e("MainActivity", "Error deleting image: " + imageUrl, e);
                        });
                }
            }
        }

        // Then delete the news document from Firestore
        db.collection("news").document(news.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم حذف الخبر بنجاح", Toast.LENGTH_SHORT).show();
                    loadNews(); // Reload the list
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error deleting news", e);
                    Toast.makeText(this, "فشل في حذف الخبر", Toast.LENGTH_SHORT).show();
                });
    }

    protected void checkLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (TextUtils.isEmpty(phone)) {
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
    @Override
    protected void showLinkedAccounts() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserId = prefs.getString("id", "");
        String phone = prefs.getString("phone", "");

        if (TextUtils.isEmpty(phone)) {
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

                        // Ne pas afficher le compte actuel
                        if (!userId.equals(currentUserId)) {
                            View accountView = getLayoutInflater().inflate(R.layout.layout_linked_account, linkedAccountsContainer, false);

                            TextView nameText = accountView.findViewById(R.id.linkedAccountName);
                            MaterialButton switchButton = accountView.findViewById(R.id.btnSwitchAccount);

                            nameText.setText(user.getFirstName() + " " + user.getLastName());

                            switchButton.setOnClickListener(v -> switchToAccount(userId));
                            linkedAccountsContainer.addView(accountView);
                        }
                    }

                    // Ouvrir le drawer après avoir chargé les comptes
                    drawerLayout.openDrawer(GravityCompat.START);
                })
                .addOnFailureListener(e -> {
                    Log.e("MainActivity", "Error loading linked accounts", e);
                    Toast.makeText(this, "خطأ في تحميل الحسابات المرتبطة", Toast.LENGTH_SHORT).show();
                });
    }

    protected void switchToAccount(String userId) {
        // Sauvegarder le nouvel ID utilisateur
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        prefs.edit().putString("id", userId).apply();

        // Redémarrer l'activité
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
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
        if (item.getItemId() == R.id.action_switch_account) {
            showLinkedAccounts();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }




    @Override
    protected void onResume() {
        super.onResume();
        // Vérifier à nouveau le rôle admin au cas où il aurait changé
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String role = prefs.getString("role", "");
        boolean wasAdmin = isAdmin;
        isAdmin = "admin".equals(role);

        if (wasAdmin != isAdmin) {
            // Le rôle a changé, mettre à jour l'interface
            if (isAdmin) {
                fab.setVisibility(View.VISIBLE);
            } else {
                fab.setVisibility(View.GONE);
            }
            // Recharger les nouvelles pour mettre à jour les boutons admin
            loadNews();
        }

        checkLinkedAccounts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Désenregistrer le receiver
        if (notificationReceiver != null) {
            LocalBroadcastManager.getInstance(this)
                    .unregisterReceiver(notificationReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}