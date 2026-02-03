package com.example.career_link_new;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    Button register_submit;
    TextInputEditText email_input, password_input, password_confirm, full_name_input;
    TextView login_redirect;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference usersRef = FirebaseDatabase
            .getInstance("https://careerlink-6ce4f-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        register_submit = findViewById(R.id.register_submit);
        email_input = findViewById(R.id.email_input);
        password_input = findViewById(R.id.password);
        password_confirm = findViewById(R.id.confirm_password);
        login_redirect = findViewById(R.id.redirect_login);
        full_name_input = findViewById(R.id.fullName_input);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();



        login_redirect.setOnClickListener(v -> {
            Intent redirect_login = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(redirect_login);
        });

        register_submit.setOnClickListener(v -> auth_user());

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // optional but recommended
                    }
                }
        );
    }

    private void auth_user() {
        String email = String.valueOf(email_input.getText()).trim();
        String password = String.valueOf(password_input.getText());
        String passwordConfirm = String.valueOf(password_confirm.getText());
        String fullName = String.valueOf(full_name_input.getText()).trim();

        if (fullName.isEmpty()) {
            full_name_input.setError("Full name required");
            full_name_input.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            email_input.setError("Please enter email");
            email_input.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            email_input.setError("Please enter valid email");
            email_input.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            password_input.setError("Please enter password");
            password_input.requestFocus();
            return;
        }

        if (password.length() < 8) {
            password_input.setError("Password should be at least 8 characters long");
            password_input.requestFocus();
            return;
        }

        if (!password.matches(".*[a-z].*")) {
            password_input.setError("Password must contain a lowercase letter");
            password_input.requestFocus();
            return;
        }

        if (!password.matches(".*[A-Z].*")) {
            password_input.setError("Password must contain an uppercase letter");
            password_input.requestFocus();
            return;
        }

        if (!password.matches(".*\\d.*")) {
            password_input.setError("Password must contain a number");
            password_input.requestFocus();
            return;
        }

        if (!password.matches(".*[!@#$%^&*()_+{}\\[\\]:;<>,.?~\\\\/-].*")) {
            password_input.setError("Password must contain a special character");
            password_input.requestFocus();
            return;
        }

        if (passwordConfirm.isEmpty()) {
            password_confirm.setError("Please repeat password");
            password_confirm.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            password_confirm.setError("Passwords do not match");
            password_input.setError("Passwords do not match");
            password_confirm.requestFocus();
            return;
        }

        // ✅ All validation passed — now Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();

                        if (e instanceof FirebaseAuthUserCollisionException) {
                            email_input.setError("Email already registered");
                            email_input.requestFocus();
                        } else {
                            Toast.makeText(
                                    this,
                                    e != null ? e.getMessage() : "Registration failed",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                        return;
                    }

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) return;

                    send_verification_email();

                    String uid = firebaseUser.getUid();

                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("fullName", fullName);
                    userMap.put("email", firebaseUser.getEmail());
                    userMap.put("createdAt", System.currentTimeMillis());

                    usersRef.child(uid).setValue(userMap)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(
                                        RegisterActivity.this,
                                        "Registration successful. Please verify your email.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                Intent intent =
                                        new Intent(RegisterActivity.this, LoginActivity.class);
                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                );
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    RegisterActivity.this,
                                    "User created but failed to save profile data",
                                    Toast.LENGTH_LONG
                            ).show());
                });
    }

    private void send_verification_email() {
        if(mAuth.getCurrentUser() != null){
            mAuth.getCurrentUser().sendEmailVerification().addOnCompleteListener(task -> {
                if(!task.isSuccessful()){
                    Toast.makeText(getApplicationContext(), "Oops! Failed to send verification email", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}