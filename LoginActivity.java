package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth; // Firebase variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);


        mAuth = FirebaseAuth.getInstance(); // Initialize Firebase

        EditText etEmail = findViewById(R.id.et_email);
        EditText etPass = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login_submit);
        TextView tvGotoLogin = findViewById(R.id.tv_goto_login);


        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Email and Password ezhudhunga!", Toast.LENGTH_SHORT).show();
            } else {
                // Firebase Login Logic
                loginUser(email, pass);
            }
        });

        // OnCreate kulla ezhudhunga
        TextView tvRegister = findViewById(R.id.tv_register_link);

        tvRegister.setOnClickListener(v -> {
            // Register page-ku pogum
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 1. Login success message
                        Toast.makeText(LoginActivity.this, "Welcome Back!", Toast.LENGTH_SHORT).show();

                        // 2. Home Activity-ku pogum logic
                        Intent intent = new Intent(LoginActivity.this, ModeSelectionActivity.class);
                        startActivity(intent);

                        // 3. Login screen-ah close panniduvom
                        finish();
                    } else {
                        // Password thappa irundha error kaattum
                        Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}