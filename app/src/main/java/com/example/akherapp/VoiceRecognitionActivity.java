package com.example.akherapp;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.VIBRATOR_SERVICE;

import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.startActivity;
import android.os.Looper;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class VoiceRecognitionActivity extends BaseUserActivity {
    private static final String TAG = "VoiceRecognition";
    private Handler handler = new Handler(Looper.getMainLooper());  // Add this line
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private static final int PERMISSION_REQUEST_CODE = 123;
    private SpeechRecognizer speechRecognizer;
    private TextView targetTextView, recognizedTextView;
    private FloatingActionButton recordButton;
    private LottieAnimationView micAnimationView;
    private ImageView successCheckmark;
    private boolean isListening = false;
    private String targetPhrase = "بسم الله الرحمن الرحيم";
//    private String targetPhrase =
//            "(١) الحمد لله الذي قد وهبـا\n" +
//                    "عبـادة المقربين الأدبـا\n\n" +
//                    "(٢) ثم الصلاة بالسلام تلتقي\n" +
//                    "علـى نبيـنا عظيـم الخلـق\n\n" +
//                    "(٣) وبعد: هاك أحسن الأخلاق\n" +
//                    "يرقُّ بها صاحبها المراقي\n\n" +
//                    "(٤) فـقـلْ: راعِ مع الله الأدب\n" +
//                    "يترك الإثم ويفعل ما وجب\n\n" +
//                    "(٥) وحسن ظنٍّ، وتوكُّلٍ، ورجا\n" +
//                    "حبٍّ، وتوحيدٍ، ومن خاف نجا\n\n" +
//                    "(٦) وقّر نبيَّه، وعظّم سنّتـه\n" +
//                    "قدِّم على الورى محبّتـه\n\n" +
//                    "(٧) وبرٍّ والديك، ولطعهما\n" +
//                    "تفزْ، وصل أيا بنيّ الرحما\n\n" +
//                    "(٨) بجلّ كبيرًا، وارحم الصغيرا\n" +
//                    "وساعد الضعيف والفقيرا\n\n" +
//                    "(٩) وكن شجاعًا، صابرًا، كريما\n" +
//                    "وشاكرًا، وفضنًا، حليمـا\n\n" +
//                    "(١٠) وذا أناةٍ، وحياءٍ، ورفق\n" +
//                    "وكن أمينًا، وتواضع، وصدق\n\n" +
//                    "(١١) وتحلَّ بالعدل، وبالإحسان\n" +
//                    "والعفو، والوفاء، والإتقان\n\n" +
//                    "(١٢) صنْ اللسان، واحفظ الجوارحا\n" +
//                    "أحبب كلَّ مسلمٍ، كن ناصحا\n\n" +
//                    "(١٣) تقبّل النصـح من اللُّقـا\n" +
//                    "كن مـن المحافظين الصلاة\n\n" +
//                    "(١٤) ووقتَك اشغلْه بالنفع التم\n" +
//                    "ولا تحفظْ لهوًا من العـدم\n\n" +
//                    "(١٥) تعلمْ بجدٍّ عن الضياع\n" +
//                    "وطالع الكتب بتزودٍ من وعي\n\n" +
//                    "(١٦) واحرصْ على كلام الحكماء\n" +
//                    "ولا تَملْ مع العلماء\n\n" +
//                    "(١٧) وأكرم العلماء طاعةً\n" +
//                    "وأقبلْ وأنصتْ للمهتدى\n\n" +
//                    "(١٨) وقدم الكبير في المجلس\n" +
//                    "وإن ألفتَ فكن صامتًا\n\n" +
//                    "(١٩) وقبل النقل خذْ تثبُّتًا\n" +
//                    "وقدّم الصلاة على الاستخارة\n\n" +
//                    "(٢٠) وإذا هممتَ فاستخرْ\n" +
//                    "واتبع الرشد، واعتذر\n\n" +
//                    "(٢١) واتبع التوبة بالغفران\n" +
//                    "واصفحْ بسلامٍ لصاحب الخوان\n\n" +
//                    "(٢٢) لا تؤذ إنسانًا، أخًا، جارا\n" +
//                    "وراعِ آداب البيتِ والدارا\n\n" +
//                    "(٢٣) وكفّ طرفك، لا تستأنسا\n" +
//                    "وأكرم الضيف، وكن مبتسما\n\n" +
//                    "(٢٤) أَصن أخاك، ثابر سلاما\n" +
//                    "كن عفيفًا، عمّا يسوء مقاما\n\n" +
//                    "(٢٥) حافظْ على الصلاة الصالحة\n" +
//                    "وكن نظيفًا، خذ الذكرَ صاحبه\n\n" +
//                    "(٢٦) وفلًا مساءً ظافحًا بالأمل\n" +
//                    "إذا عطست فحمدتَ لله\n\n" +
//                    "(٢٧) لا تشمتْ من أحدٍ لا لام\n" +
//                    "طَهّرْ لسانك من الكلام الحرام\n\n" +
//                    "(٢٨) وكنْ من الذاكرين في المنام\n" +
//                    "وقمْ بالليل، وبلّغ سلام\n\n" +
//                    "(٢٩) فافعل الخير، ثم احمدْ الله\n" +
//                    "وصلِّ على من أرسله الله\n\n" +
//                    "(٣٠) نبينا المختار في بلاغ\n" +
//                    "نظم الفقير إلى الله، بهجة عمر";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recognition);

        // Show development dialog
        showDevelopmentDialog();


        setupViews();
        setupNavigationDrawer();
        // Highlight the active menu item
        navigationView.setCheckedItem(R.id.menu_voice_recognition);

        initializeViews();
        setupSpeechRecognizer();
        setupClickListeners();

        targetTextView.setText(targetPhrase);
        targetTextView.setTextIsSelectable(true);  // Make text selectable
    }


    private void showDevelopmentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("تنبيه")
                .setMessage("هذه الميزة قيد التطوير حاليا")
                .setPositiveButton("موافق", (dialog, which) -> {
                    dialog.dismiss();
                    // Continue with normal activity flow
                })
                .setCancelable(false)
                .show();
    }
    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("تدريب النطق");
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
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
            } else if (id == R.id.menu_progress) {
                startActivity(new Intent(this, ProgressTrackingActivity.class));
            } else if (id == R.id.menu_documents) {
                startActivity(new Intent(this, DocumentUploadActivity.class));
            } else if (id == R.id.menu_submit_complaint) {
                startActivity(new Intent(this, SubmitComplaintActivity.class));
            } else if (id == R.id.menu_logout) {
                getSharedPreferences("user_prefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Load user data in nav header
        loadUserDataInNavHeader();
    }

    private void loadUserDataInNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView nameView = headerView.findViewById(R.id.nav_header_name);
        TextView roleView = headerView.findViewById(R.id.nav_header_role);
        TextView phoneView = headerView.findViewById(R.id.nav_header_phone);
        ShapeableImageView profileImageView = headerView.findViewById(R.id.nav_header_image);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = prefs.getString("id", null);

        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                String fullName = user.getFirstName() + " " + user.getLastName();
                                nameView.setText(fullName.trim());
                                phoneView.setText(user.getPhone());
                                roleView.setText("طالب");

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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initializeViews() {
        targetTextView = findViewById(R.id.targetTextView);
        recognizedTextView = findViewById(R.id.recognizedTextView);
        recordButton = findViewById(R.id.recordButton);
        micAnimationView = findViewById(R.id.micAnimationView);
        successCheckmark = findViewById(R.id.successCheckmark);
    }

    private void setupSpeechRecognizer() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
                recognizedTextView.setText("");
                isListening = true;
            }

            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "Got results");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d(TAG, "Recognized text: " + recognizedText);

                    // Set the text immediately first
                    recognizedTextView.setText(recognizedText);

                    // Then animate it
                    handler.postDelayed(() -> {
                        ((CustomAnimatedTextView) recognizedTextView).animateTextWordByWord(recognizedText);
                    }, 500);

                    String normalizedRecognized = normalizeArabicText(recognizedText);
                    String normalizedTarget = normalizeArabicText(targetPhrase);

                    if (normalizedRecognized.equals(normalizedTarget)) {
                        showSuccess();
                    }
                } else {
                    Log.d(TAG, "No recognition results");
                    recognizedTextView.setText("لم يتم التعرف على الكلام");
                }
                cleanupRecognizer();
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of speech");
            }

            @Override
            public void onError(int error) {
                Log.e(TAG, "Speech recognition error: " + getErrorText(error));
                String errorMessage;
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        errorMessage = "لم يتم التعرف على الكلام";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        errorMessage = "تحقق من اتصال الإنترنت";
                        break;
                    default:
                        errorMessage = "حدث خطأ، حاول مرة أخرى";
                }
                recognizedTextView.setText(errorMessage);
                cleanupRecognizer();
            }

            // Required overrides
            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String partialText = matches.get(0);
                    if (!partialText.trim().isEmpty()) {
                        recognizedTextView.setText(partialText);
                    }
                }
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    private String normalizeArabicText(String text) {
        if (text == null) return "";

        // Remove diacritics (tashkeel)
        text = text.replaceAll("[\u064B-\u065F\u0670]", "");

        // Normalize alef variations to simple alef
        text = text.replaceAll("[\u0622\u0623\u0625]", "\u0627");

        // Normalize teh marbuta to heh
        text = text.replace("\u0629", "\u0647");

        // Remove non-Arabic characters and extra whitespace
        text = text.replaceAll("[^\\u0621-\\u063A\\u0641-\\u064A\\s]", "");

        return text.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    private void setupClickListeners() {
        recordButton.setOnClickListener(v -> {
            if (checkPermission()) {
                if (!isListening) {
                    startListening();
                } else {
                    stopListening();
                }
            } else {
                requestPermission();
            }
        });
    }

    private void startListening() {
        setupSpeechRecognizer();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA"); // Specific Arabic locale
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);

        try {
            isListening = true;
            recordButton.setImageResource(R.drawable.ic_stop);
            micAnimationView.playAnimation();
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting speech recognition", e);
            cleanupRecognizer();
            Toast.makeText(this, "خطأ في بدء التسجيل", Toast.LENGTH_SHORT).show();
        }
    }


    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        cleanupRecognizer();
    }

    private void cleanupRecognizer() {
        isListening = false;
        recordButton.setImageResource(R.drawable.ic_mic);
        micAnimationView.pauseAnimation();
        micAnimationView.cancelAnimation();
        micAnimationView.setProgress(0);

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cleanupRecognizer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupRecognizer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            }
        }
    }

    // Add after cleanupRecognizer() method
    private void showSuccess() {
        successCheckmark.setVisibility(View.VISIBLE);
        successCheckmark.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(200);
        }

        // Hide checkmark after delay
        new Handler().postDelayed(() -> {
            successCheckmark.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
            successCheckmark.setVisibility(View.GONE);
        }, 2000);
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Remove duplicate setupSpeechRecognizer() method and keep only one
    // Fix the requestPermission() method syntax
    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE
        );
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "Error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Unknown error";
                break;
        }
        return message;
    }
}