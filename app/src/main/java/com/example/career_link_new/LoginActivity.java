package com.example.career_link_new;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    TextInputEditText Email, Password;
    Button Submit;
    TextView register_button;
    LinearLayout login_layout;
    String email, password;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null && currentUser.isEmailVerified()) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        Email = findViewById(R.id.email_input);
        Password = findViewById(R.id.password);
        Submit = findViewById(R.id.login_submit);
        register_button = findViewById(R.id.register);
        login_layout = findViewById(R.id.linearLayout);

        register_button.setOnClickListener(v -> {
            try{
                Intent redirect_register = new Intent(this, RegisterActivity.class);
                LoginActivity.this.startActivity(redirect_register);
            }
            catch (Exception e) {
                Log.e("LoginActivity", "Failed to open RegisterActivity", e);
                Toast.makeText(this,
                        "Something went wrong. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        login_layout.setAlpha(0.8F);

        Submit.setOnClickListener(v -> perform_login());

        getOnBackPressedDispatcher().addCallback(
                this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Completely block back navigation
                        finishAffinity(); // closes the app instead of going back
                    }
                }
        );
    }

    private void perform_login() {
        email = Objects.requireNonNull(Email.getText()).toString().trim();
        password = Objects.requireNonNull(Password.getText()).toString();

        if(email.isEmpty()){
            Email.setError("Enter email");
            Email.requestFocus();
            return;
        }

        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            Email.setError("Email not valid");
            Email.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Password.setError("Enter password");
            Password.requestFocus();
            return;
        }

        if (password.length() < 8) {
            Password.setError("Password must be at least 8 characters");
            Password.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user == null) return;

                        if (!user.isEmailVerified()) {
                            Toast.makeText(
                                    this,
                                    "Please verify your email before logging in",
                                    Toast.LENGTH_LONG
                            ).show();
                            mAuth.signOut();
                            return;
                        }

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        send_user_to_next_activity();
                    } else {
                        Toast.makeText(
                                this,
                                "Invalid email or password",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }
    private void send_user_to_next_activity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}