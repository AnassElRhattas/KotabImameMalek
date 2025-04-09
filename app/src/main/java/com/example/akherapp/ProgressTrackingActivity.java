package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgressTrackingActivity extends BaseUserActivity {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LineChart chart;
    private FirebaseFirestore db;
    private String userId;
    private RecyclerView achievementsRecyclerView;
    private RecyclerView rankingRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_tracking);

        db = FirebaseFirestore.getInstance();
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = prefs.getString("id", null);

        setupViews();
        setupNavigationDrawer();
        // Mettre en évidence l'élément de menu actif
        navigationView.setCheckedItem(R.id.menu_progress);
        loadProgressData();

        // Ajouter ces deux lignes
        setupAchievements();
        loadRankingData();
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("تتبع الحفظ");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        chart = findViewById(R.id.lineChart);

        // Configuration du graphique
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(true);
        chart.setBorderColor(Color.parseColor("#E0E0E0"));
        chart.setBorderWidth(2f);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setNoDataText("جاري تحميل البيانات...");
        chart.setNoDataTextColor(getResources().getColor(R.color.colorPrimary));
        chart.setExtraOffsets(15f, 15f, 15f, 15f);
        chart.animateX(1500);

        // Initialiser une liste vide pour les entrées
        ArrayList<Entry> entries = new ArrayList<>();

        // Style de la ligne
        LineDataSet dataSet = new LineDataSet(entries, "تقدم الحفظ");
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setCircleRadius(6f);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setDrawCircleHole(true);
        dataSet.setValueTextSize(12f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setDrawFilled(true);
        dataSet.setFillDrawable(getResources().getDrawable(R.drawable.gradient_chart));
        dataSet.setHighlightEnabled(true);
        dataSet.setHighLightColor(Color.parseColor("#80" +
                Integer.toHexString(getResources().getColor(R.color.colorPrimary)).substring(2)));

        // Configuration des axes X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setGridLineWidth(1f);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setAxisLineColor(Color.parseColor("#757575"));
        xAxis.setAxisLineWidth(2f);

        // Configuration des axes Y
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setTextSize(12f);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisLineColor(Color.parseColor("#757575"));
        leftAxis.setAxisLineWidth(2f);

        // Désactiver l'axe droit
        chart.getAxisRight().setEnabled(false);

        // Configuration de la légende
        Legend legend = chart.getLegend();
        legend.setEnabled(true);
        legend.setTextSize(14f);
        legend.setTextColor(Color.BLACK);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setFormSize(10f);
        legend.setXEntrySpace(10f);
        legend.setYEntrySpace(5f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
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
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.menu_schedule) {
                startActivity(new Intent(this, ViewScheduleActivity.class));
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
                finish();
            } else if (id == R.id.menu_progress) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.menu_voice_recognition) {
                startActivity(new Intent(this, VoiceRecognitionActivity.class));
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_submit_complaint) {
                startActivity(new Intent(this, SubmitComplaintActivity.class));
                finish();
            } else if (id == R.id.menu_logout) {
                // Effacer les préférences et rediriger vers LoginActivity
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            return true;
        });

        // Charger les données utilisateur dans le nav header
        View headerView = navigationView.getHeaderView(0);
        TextView nameView = headerView.findViewById(R.id.nav_header_name);
        TextView roleView = headerView.findViewById(R.id.nav_header_role);
        TextView phoneView = headerView.findViewById(R.id.nav_header_phone);
        ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // Afficher le nom complet
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                nameView.setText(fullName.trim());
                                phoneView.setText(user.getPhone());
                                // Afficher le rôle
                                roleView.setText("طالب");

                                // Charger l'image de profil
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
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "خطأ في تحميل معلومات المستخدم", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadProgressData() {
        String userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("id", "");
        if (userId.isEmpty()) {
            Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        ArrayList<Entry> entries = new ArrayList<>();
                        float totalProgress = 0f;

                        // Semaine 1
                        float week1Progress = user.getWeek1Progress() != null ? user.getWeek1Progress() : 0f;
                        entries.add(new Entry(0, week1Progress));
                        totalProgress += week1Progress;

                        // Semaine 2
                        float week2Progress = user.getWeek2Progress() != null ? user.getWeek2Progress() : 0f;
                        entries.add(new Entry(1, week2Progress));
                        totalProgress += week2Progress;

                        // Semaine 3
                        float week3Progress = user.getWeek3Progress() != null ? user.getWeek3Progress() : 0f;
                        entries.add(new Entry(2, week3Progress));
                        totalProgress += week3Progress;

                        // Semaine 4
                        float week4Progress = user.getWeek4Progress() != null ? user.getWeek4Progress() : 0f;
                        entries.add(new Entry(3, week4Progress));
                        totalProgress += week4Progress;

                        // Afficher le total
                        TextView totalProgressText = findViewById(R.id.totalProgressText);
                        totalProgressText.setText(String.format("المجموع الكلي: %.0f آية", totalProgress));

                        LineDataSet dataSet = new LineDataSet(entries, "عدد الآيات المحفوظة");
                        dataSet.setColor(Color.BLUE);
                        dataSet.setValueTextColor(Color.BLACK);
                        dataSet.setValueTextSize(12f);
                        dataSet.setLineWidth(2f);
                        dataSet.setCircleColor(Color.BLUE);
                        dataSet.setCircleRadius(4f);
                        dataSet.setDrawValues(true);

                        LineData lineData = new LineData(dataSet);
                        chart.setData(lineData);
                        chart.getDescription().setEnabled(false);
                        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        chart.getXAxis().setGranularity(1f);
                        chart.getXAxis().setValueFormatter(new ValueFormatter() {
                            private final String[] weeks = new String[]{"الأسبوع 1", "الأسبوع 2", "الأسبوع 3", "الأسبوع 4"};
                            @Override
                            public String getFormattedValue(float value) {
                                int index = (int) value;
                                if (index >= 0 && index < weeks.length) {
                                    return weeks[index];
                                }
                                return "";
                            }
                        });
                        chart.animateY(1000);
                        chart.invalidate();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "خطأ في تحميل البيانات", Toast.LENGTH_SHORT).show();
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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupAchievements() {
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);
        achievementsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        if (userId == null) return;

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    List<Achievement> achievements = new ArrayList<>();
                    float totalProgress = 0f;

                    if (user != null) {
                        if (user.getWeek1Progress() != null) totalProgress += user.getWeek1Progress();
                        if (user.getWeek2Progress() != null) totalProgress += user.getWeek2Progress();
                        if (user.getWeek3Progress() != null) totalProgress += user.getWeek3Progress();
                        if (user.getWeek4Progress() != null) totalProgress += user.getWeek4Progress();
                    }

                    // Always add badges regardless of progress
                    achievements.add(new Achievement("مائة آية", R.drawable.badge_100, totalProgress, 100f));
                    achievements.add(new Achievement("خمسون آية", R.drawable.badge_50, totalProgress, 50f));

                    float improvementProgress = 0f;
                    if (user != null) {
                        Float week3Progress = user.getWeek3Progress();
                        Float week4Progress = user.getWeek4Progress();
                        if (week3Progress != null && week4Progress != null) {
                            improvementProgress = week4Progress > week3Progress ? 100f :
                                    (week4Progress / week3Progress) * 100f;
                        }
                    }
                    achievements.add(new Achievement("تحسن مستمر", R.drawable.badge_improvement,
                            improvementProgress, 100f));

                    AchievementsAdapter adapter = new AchievementsAdapter(achievements);
                    achievementsRecyclerView.setAdapter(adapter);
                    achievementsRecyclerView.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "خطأ في تحميل الإنجازات", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadRankingData() {
        rankingRecyclerView = findViewById(R.id.rankingRecyclerView);
        rankingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Get current user ID
        String currentUserId = getSharedPreferences("user_prefs", MODE_PRIVATE).getString("id", "");

        // Récupérer uniquement les utilisateurs avec le rôle "student"
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserRanking> rankings = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            // Set the user ID
                            user.setId(document.getId());
                            float totalProgress = 0f;
                            Float week1 = user.getWeek1Progress();
                            Float week2 = user.getWeek2Progress();
                            Float week3 = user.getWeek3Progress();
                            Float week4 = user.getWeek4Progress();

                            if (week1 != null) totalProgress += week1;
                            if (week2 != null) totalProgress += week2;
                            if (week3 != null) totalProgress += week3;
                            if (week4 != null) totalProgress += week4;

                            // Ajouter seulement si l'utilisateur a du progrès
                            if (totalProgress > 0) {
                                rankings.add(new UserRanking(user, totalProgress));
                            }
                        }
                    }

                    // Trier par progrès total (du plus grand au plus petit)
                    Collections.sort(rankings, (a, b) ->
                            Float.compare(b.getTotalProgress(), a.getTotalProgress()));

                    if (!rankings.isEmpty()) {
                        RankingAdapter adapter = new RankingAdapter(rankings, currentUserId);
                        rankingRecyclerView.setAdapter(adapter);
                        rankingRecyclerView.setVisibility(View.VISIBLE);
                    } else {
                        rankingRecyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "خطأ في تحميل الترتيب", Toast.LENGTH_SHORT).show();
                    Log.e("Ranking", "Error loading rankings", e);
                });
    }
}
