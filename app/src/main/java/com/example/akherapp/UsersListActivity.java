package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
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
import com.example.akherapp.utils.DateUtils;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
// Modifier l'import de RadioGroup à ChipGroup
import com.google.android.material.chip.ChipGroup;
//import com.google.android.material.widget.RadioButton;
//import com.google.android.material.widget.RadioGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import android.app.ProgressDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.akherapp.utils.PdfExporter;

public class UsersListActivity extends BaseActivity {
    private RecyclerView usersRecyclerView;
    private ProgressBar progressBar;
    private UsersListAdapter usersListAdapter;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextInputEditText searchInput;

    // Supprimer l'import de RadioGroup
    // import android.widget.RadioGroup;

    private ChipGroup sortGroup;
    // Change the field type from Spinner to AutoCompleteTextView
    private AutoCompleteTextView teacherFilter;

    private List<User> allUsers = new ArrayList<>();
    private List<String> teachers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_list);

        // Add Google Play Services check
        if (!checkGooglePlayServices()) {
            Toast.makeText(this, "Please install Google Play Services to use this app", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        initializeViews();
        setupToolbar();
        setupNavigationDrawer();
        setupRecyclerView();
        setupSortingAndFiltering();
        setupSearch();
        loadUsers();

        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_users);
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("إدارة المستخدمين");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        searchInput = findViewById(R.id.searchInput);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        usersRecyclerView = findViewById(R.id.usersRecyclerView);
        progressBar = findViewById(R.id.progressBar);

        sortGroup = findViewById(R.id.sortGroup);
        teacherFilter = findViewById(R.id.teacherFilter);
        // Supprimer cette ligne car le FAB n'existe plus
        // FloatingActionButton exportPdfFab = findViewById(R.id.exportPdfFab);
        // exportPdfFab.setOnClickListener(v -> exportUsersToPdf());
        findViewById(R.id.generateAllCertificatesButton).setOnClickListener(v -> generateAllCertificates());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("إدارة المستخدمين");
        }
    }

    private void setupNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("isAdmin", true);
                startActivity(intent);
                finish();
            } else if (id == R.id.menu_add_news) {
                startActivity(new Intent(this, AdminActivity.class));
                finish();
            } else if (id == R.id.menu_contact_users) {
                startActivity(new Intent(this, ContactUsersActivity.class));
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ScheduleManagementActivity.class));
            } else if (id == R.id.menu_verify_documents) {
                startActivity(new Intent(this, VerifyDocumentsActivity.class));
                finish();
            }  else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_complaints) {
                startActivity(new Intent(this, ManageComplaintsActivity.class));
                finish();
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, AdminProfileActivity.class));
                finish();
            } else if (id == R.id.menu_logout) {
                // Effacer les données de session
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                // Rediriger vers LoginActivity et effacer la pile d'activités
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

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
                                TextView roleView = headerView.findViewById(R.id.nav_header_role);
                                ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

                                // Afficher le nom complet
                                String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                                        (user.getLastName() != null ? user.getLastName() : "");
                                if (nameView != null) {
                                    nameView.setText(fullName.trim());
                                }

                                // Afficher le rôle
                                if (roleView != null) {
                                    roleView.setText("مدير النظام");
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
                        Toast.makeText(this,
                                "خطأ في تحميل معلومات المستخدم",
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupRecyclerView() {
        usersListAdapter = new UsersListAdapter(new ArrayList<>());
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(usersListAdapter);
    }

    private void setupSortingAndFiltering() {
        // Configuration du tri
        sortGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.sortByName) {
                sortUsersByName();
            } else if (checkedId == R.id.sortByAge) {
                sortUsersByAge();
            } else if (checkedId == R.id.sortByTeacher) {
                sortUsersByTeacher();
            }
        });

        // Configuration du filtre par maître
        ArrayAdapter<String> teacherAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, teachers);
        teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teacherFilter.setAdapter(teacherAdapter);

        teacherFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTeacher = teachers.get(position);
                filterUsersByTeacher(selectedTeacher);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Réinitialiser le filtre
                usersListAdapter.updateUsers(allUsers);
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usersListAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allUsers.clear();
                    teachers.clear();
                    teachers.add("الكل"); // Ajouter l'option "Tous"

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            allUsers.add(user);
                            if (user.getTeacher() != null && !teachers.contains(user.getTeacher())) {
                                teachers.add(user.getTeacher());
                            }
                        }
                    }

                    // Update the adapter
                    ((ArrayAdapter<String>) teacherFilter.getAdapter()).notifyDataSetChanged();
                    teacherFilter.setText("الكل", false); // Set default selection without filtering

                    // Apply default sorting (by name)
                    sortUsersByName();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(UsersListActivity.this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void sortUsersByName() {
        Collections.sort(allUsers, (u1, u2) -> {
            String name1 = (u1.getFirstName() + " " + u1.getLastName()).trim();
            String name2 = (u2.getFirstName() + " " + u2.getLastName()).trim();
            return name1.compareTo(name2);
        });
        usersListAdapter.updateUsers(allUsers);
    }

    private void sortUsersByAge() {
        Collections.sort(allUsers, (u1, u2) -> {
            int age1 = DateUtils.calculateAge(u1.getBirthDate());
            int age2 = DateUtils.calculateAge(u2.getBirthDate());
            return Integer.compare(age1, age2);
        });
        usersListAdapter.updateUsers(allUsers);
    }

    private void sortUsersByTeacher() {
        Collections.sort(allUsers, (u1, u2) -> {
            String teacher1 = u1.getTeacher() != null ? u1.getTeacher() : "";
            String teacher2 = u2.getTeacher() != null ? u2.getTeacher() : "";
            return teacher1.compareTo(teacher2);
        });
        usersListAdapter.updateUsers(allUsers);
    }

    private void filterUsersByTeacher(String teacher) {
        if (teacher.equals("الكل")) {
            usersListAdapter.updateUsers(allUsers);
            return;
        }

        List<User> filteredUsers = allUsers.stream()
                .filter(user -> teacher.equals(user.getTeacher()))
                .collect(Collectors.toList());
        usersListAdapter.updateUsers(filteredUsers);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    private boolean checkGooglePlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
            }
            return false;
        }
        return true;
    }


    private void exportUsersToPdf() {
        try {
            File pdfFile = PdfExporter.exportUsersToPdf(this, allUsers);

            // Ouvrir le PDF avec une application externe
            Uri pdfUri = FileProvider.getUriForFile(this,
                    "com.example.akherapp.fileprovider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "يرجى تثبيت تطبيق لقراءة ملفات PDF", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "حدث خطأ أثناء إنشاء ملف PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_export_pdf) {
            exportUsersToPdf();
            return true;
        } else if (id == R.id.action_notifications) {
            // Gérer les notifications
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void generateAllCertificates() {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("جاري إنشاء الشهادات...");
            progressDialog.setCancelable(false);
            progressDialog.show();
    
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
    
            executor.execute(() -> {
                try {
                    File certificatesFile = CertificateGenerator.generateAllCertificates(this, allUsers);
                    handler.post(() -> {
                        progressDialog.dismiss();
                        openPdfFile(certificatesFile);
                        Toast.makeText(this, "تم إنشاء الشهادات بنجاح", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "فشل في إنشاء الشهادات", Toast.LENGTH_SHORT).show();
                    });
                    e.printStackTrace();
                }
            });
        }
    
        private void openPdfFile(File file) {
            Uri uri = FileProvider.getUriForFile(this, 
                "com.example.akherapp.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "لا يوجد تطبيق لفتح ملف PDF", Toast.LENGTH_SHORT).show();
            }
        }
}
