package com.example.akherapp;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.akherapp.utils.NotificationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.app.ProgressDialog;
import android.text.format.DateFormat;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import java.util.Locale;

public class SignupActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private TextInputEditText firstNameInput, lastNameInput, phoneInput, passwordInput, confirmPasswordInput;
    private EditText birthdateInput;
    private AutoCompleteTextView teacherSpinner;
    private String[] teachers = {"حميد الحماني", "عبد الغني بياض"};
    private String selectedTeacher;
    private String birthDateMillis;
    private MaterialButton signupButton, loginButton;
    private Uri selectedImageUri;
    private FirebaseFirestore db; // Add this line

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        db = FirebaseFirestore.getInstance(); // Initialize db here
        initializeViews();
        setupListeners();
        setupTeacherSpinner();
    }

    // Ajouter ces variables de classe
    // Modifier la déclaration de la variable
    private ShapeableImageView profileImageView;
    private MaterialButton btnSelectImage;  // Changer FloatingActionButton en MaterialButton

    // Ajouter dans les variables de classe
    private TextInputEditText  fatherFirstNameInput, fatherLastNameInput;

    private void initializeViews() {
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        fatherFirstNameInput = findViewById(R.id.fatherFirstNameInput);
        fatherLastNameInput = findViewById(R.id.fatherLastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        birthdateInput = findViewById(R.id.birthdateInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        teacherSpinner = findViewById(R.id.teacherSpinner);
        signupButton = findViewById(R.id.signupButton);
        loginButton = findViewById(R.id.loginButton);
        profileImageView = findViewById(R.id.profileImageView);
        btnSelectImage = findViewById(R.id.btnSelectImage);  // Maintenant compatible avec MaterialButton
    }

    private void setupListeners() {
        birthdateInput.setOnClickListener(v -> showDatePicker());
        signupButton.setOnClickListener(v -> validateAndSignup());
        loginButton.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
        btnSelectImage.setOnClickListener(v -> selectImage());  // Ajouter le listener ici
    }

    private void setupTeacherSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                teachers
        );
        teacherSpinner.setAdapter(adapter);

        // Remplacer le listener précédent par celui-ci
        teacherSpinner.setOnItemClickListener((parent, view, position, id) -> {
            selectedTeacher = teachers[position];
        });

        // Définir la valeur par défaut
        selectedTeacher = teachers[0];
        teacherSpinner.setText(selectedTeacher, false);
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "اختر صورة"), PICK_IMAGE_REQUEST);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar birthDate = Calendar.getInstance();
                    birthDate.set(selectedYear, selectedMonth, selectedDay);
                    birthDate.set(Calendar.HOUR_OF_DAY, 0);
                    birthDate.set(Calendar.MINUTE, 0);
                    birthDate.set(Calendar.SECOND, 0);
                    birthDate.set(Calendar.MILLISECOND, 0);

                    birthDateMillis = String.valueOf(birthDate.getTimeInMillis());

                    // Format date for display
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    birthdateInput.setText(sdf.format(birthDate.getTime()));
                },
                year - 18, // Default to 18 years ago
                month,
                day
        );

        // Set max date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showExistingNameDialog(User existingUser) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_existing_name);

        ShapeableImageView existingUserImage = dialog.findViewById(R.id.existingUserImage);
        TextView existingUserName = dialog.findViewById(R.id.existingUserName);
        MaterialButton btnLogin = dialog.findViewById(R.id.btnLogin);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        existingUserName.setText(existingUser.getFirstName() + " " + existingUser.getLastName());
        
        if (existingUser.getProfileImageUrl() != null && !existingUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(existingUser.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(existingUserImage);
        }

        btnLogin.setOnClickListener(v -> {
            dialog.dismiss();
            // Redirect to login with pre-filled name
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("firstName", existingUser.getFirstName());
            intent.putExtra("lastName", existingUser.getLastName());
            startActivity(intent);
            finish();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void validateAndSignup() {
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String fatherFirstName = fatherFirstNameInput.getText().toString().trim();
        String fatherLastName = fatherLastNameInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || fatherFirstName.isEmpty() ||
                fatherLastName.isEmpty() || phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check for name duplicates first
        db.collection("users")
            .whereEqualTo("firstName", firstName)
            .whereEqualTo("lastName", lastName)
            .get()
            .addOnSuccessListener(nameQuerySnapshot -> {
                if (!nameQuerySnapshot.isEmpty()) {
                    // Name exists, show dialog
                    User existingUser = nameQuerySnapshot.getDocuments().get(0).toObject(User.class);
                    showExistingNameDialog(existingUser);
                } else {
                    // Name is unique, check phone
                    checkPhoneAndProceed(firstName, lastName, phone, password);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "حدث خطأ. يرجى المحاولة مرة أخرى", Toast.LENGTH_SHORT).show();
            });
    }

    private void checkPhoneAndProceed(String firstName, String lastName, String phone, String password) {
        db.collection("users")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener(phoneQuerySnapshot -> {
                if (!phoneQuerySnapshot.isEmpty()) {
                    showExistingPhoneDialog(phoneQuerySnapshot.getDocuments().get(0).toObject(User.class));
                } else {
                    proceedWithSignup(firstName, lastName, phone, password);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "حدث خطأ. يرجى المحاولة مرة أخرى", Toast.LENGTH_SHORT).show();
            });
    }

    private void showExistingPhoneDialog(User existingUser) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_existing_phone);

        ShapeableImageView existingUserImage = dialog.findViewById(R.id.existingUserImage);
        TextView existingUserName = dialog.findViewById(R.id.existingUserName);
        TextView existingUserPhone = dialog.findViewById(R.id.existingUserPhone);
        MaterialButton btnAccessExistingAccount = dialog.findViewById(R.id.btnAccessExistingAccount);
        MaterialButton btnContinueRegistration = dialog.findViewById(R.id.btnContinueRegistration);

        existingUserName.setText(existingUser.getFirstName() + " " + existingUser.getLastName());
        existingUserPhone.setText(existingUser.getPhone());
        if (existingUser.getProfileImageUrl() != null && !existingUser.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(existingUser.getProfileImageUrl())
                    .placeholder(R.drawable.default_profile_image)
                    .error(R.drawable.default_profile_image)
                    .into(existingUserImage);
        }

        btnAccessExistingAccount.setOnClickListener(v -> {
            dialog.dismiss();
            // Redirect to login with pre-filled name
            Intent intent = new Intent(this, LoginActivity.class);
            intent.putExtra("firstName", existingUser.getFirstName());
            intent.putExtra("lastName", existingUser.getLastName());
            startActivity(intent);
            finish();
        });

        btnContinueRegistration.setOnClickListener(v -> {
            dialog.dismiss();
            String firstName = firstNameInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            proceedWithSignup(firstName, lastName, phone, password);
        });

        dialog.show();
    }

    private void proceedWithSignup(String firstName, String lastName, String phone, String password) {
        // Validation des champs
        if (!validateFields()) {
            return;
        }

        // Afficher un dialogue de progression
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("جاري إنشاء الحساب...");
        progressDialog.show();

        // Obtenir une référence à Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();

        // Si une image a été sélectionnée, la télécharger d'abord
        if (selectedImageUri != null) {
            StorageReference storageRef = storage.getReference()
                    .child("profile_images")
                    .child(UUID.randomUUID().toString());

            storageRef.putFile(selectedImageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return storageRef.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult();
                            createUser(db, downloadUri.toString(), progressDialog, firstName, lastName, phone, password);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(SignupActivity.this,
                                    "فشل في تحميل الصورة", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Pas d'image sélectionnée, sauvegarder l'utilisateur sans image
            createUser(db, null, progressDialog, firstName, lastName, phone, password);
        }
    }

    private void createUser(FirebaseFirestore db, String profileImageUrl, ProgressDialog progressDialog,
                            String firstName, String lastName, String phone, String password) {
        String userId = UUID.randomUUID().toString();
        String birthDate = String.valueOf(birthDateMillis);
        String teacher = selectedTeacher;
        String fatherFirstName = fatherFirstNameInput.getText().toString().trim();
        String fatherLastName = fatherLastNameInput.getText().toString().trim();

        User newUser = new User();
        newUser.setId(userId);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setFatherFirstName(fatherFirstName);
        newUser.setFatherLastName(fatherLastName);
        newUser.setPhone(phone);
        newUser.setBirthDate(birthDate);
        newUser.setProfileImageUrl(profileImageUrl);
        newUser.setTeacher(teacher);
        newUser.setRole("student");
        newUser.setPassword(password);
        // Ajouter la date de création
        newUser.setRegistrationDate(new Date());

        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();

                    // Envoyer une notification à l'admin
                    String fullName = firstName + " " + lastName;
                    NotificationUtils.sendNewUserNotification(fullName);

                    Toast.makeText(SignupActivity.this,
                            "تم إنشاء الحساب بنجاح", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignupActivity.this,
                            "فشل في إنشاء الحساب: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateFields() {
        boolean isValid = true;

        String phone = phoneInput.getText().toString().trim();
        if (phone.isEmpty()) {
            phoneInput.setError("الرجاء إدخال رقم الهاتف");
            isValid = false;
        } else if (phone.length() != 10) {
            phoneInput.setError("يجب أن يتكون رقم الهاتف من 10 أرقام");
            isValid = false;
        } else if (!phone.matches("^[0-9]{10}$")) {
            phoneInput.setError("يجب أن يحتوي رقم الهاتف على أرقام فقط");
            isValid = false;
        }

        if (firstNameInput.getText().toString().trim().isEmpty()) {
            firstNameInput.setError("الرجاء إدخال الاسم الأول");
            isValid = false;
        }

        if (lastNameInput.getText().toString().trim().isEmpty()) {
            lastNameInput.setError("الرجاء إدخال اسم العائلة");
            isValid = false;
        }

        if (fatherFirstNameInput.getText().toString().trim().isEmpty()) {
            fatherFirstNameInput.setError("الرجاء إدخال اسم الأب");
            isValid = false;
        }

        if (fatherLastNameInput.getText().toString().trim().isEmpty()) {
            fatherLastNameInput.setError("الرجاء إدخال لقب الأب");
            isValid = false;
        }

        if (phoneInput.getText().toString().trim().isEmpty()) {
            phoneInput.setError("الرجاء إدخال رقم الهاتف");
            isValid = false;
        }

        if (birthdateInput.getText().toString().trim().isEmpty()) {
            birthdateInput.setError("الرجاء إدخال تاريخ الميلاد");
            isValid = false;
        }

        if (passwordInput.getText().toString().trim().isEmpty()) {
            passwordInput.setError("الرجاء إدخال كلمة المرور");
            isValid = false;
        }

        if (!passwordInput.getText().toString().trim().equals(confirmPasswordInput.getText().toString().trim())) {
            confirmPasswordInput.setError("كلمة المرور غير متطابقة");
            isValid = false;
        }

        if (selectedTeacher == null) {
            Toast.makeText(this, "يرجى اختيار المعلم", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            // Mettre à jour l'image avec Glide
            Glide.with(this)
                    .load(selectedImageUri)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .centerCrop()
                    .into(profileImageView);
        }
    }
}