package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.akherapp.adapters.UserContactAdapter;
import com.example.akherapp.User;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ContactUsersActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    private RecyclerView usersRecyclerView;
    private UserContactAdapter adapter;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private EditText searchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_users);

        // Initialiser Firestore
        db = FirebaseFirestore.getInstance();

        // Configurer la toolbar et le drawer
        setupToolbarAndDrawer();

        // Configurer le RecyclerView
        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserContactAdapter(this, new ArrayList<>());
        usersRecyclerView.setAdapter(adapter);

        // Configurer la recherche
        setupSearch();

        // Charger les utilisateurs
        loadUsers();

        // Charger les informations de l'utilisateur dans le header
        loadUserInfoInHeader();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_contact_users);
    }

    private void setupSearch() {
        searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void loadUserInfoInHeader() {
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
                            TextView roleView = headerView.findViewById(R.id.nav_header_role);

                            String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                           (user.getLastName() != null ? user.getLastName() : "");
                            nameView.setText(fullName.trim());
                            roleView.setText("مدير النظام");

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
                });
        }
    }

    private void loadUsers() {
        db.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> users = new ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    if (user != null && user.getPhone() != null && !user.getPhone().isEmpty()) {
                        users.add(user);
                    }
                }
                adapter.updateUsers(users);
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "فشل في تحميل قائمة المستخدمين", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        Intent intent = null;

        if (id == R.id.menu_home) {
            intent = new Intent(this, MainActivity.class);
        } else if (id == R.id.menu_add_news) {
            intent = new Intent(this, AdminActivity.class);
        } else if (id == R.id.menu_schedule) {
            intent = new Intent(this, ScheduleManagementActivity.class);
        } else if (id == R.id.menu_users) {
            intent = new Intent(this, UsersListActivity.class);
        } else if (id == R.id.menu_verify_documents) {
            startActivity(new Intent(this, VerifyDocumentsActivity.class));
            finish();
        } else if (id == R.id.menu_contact_users) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (id == R.id.menu_complaints) {
            intent = new Intent(this, ManageComplaintsActivity.class);
        } else if (id == R.id.menu_profile) {
            intent = new Intent(this, AdminProfileActivity.class);
        } else if (id == R.id.menu_logout) {
            SharedPreferences.Editor editor = getSharedPreferences("user_prefs", MODE_PRIVATE).edit();
            editor.clear();
            editor.apply();
            intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        if (intent != null) {
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
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
