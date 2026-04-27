package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Welcomepage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcomepage);
        // Welcomepage.java kulla

        Button btnLogin = findViewById(R.id.btn_login);

        Button btnLogin2 = findViewById(R.id.btn_register);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Login Page-ku poga Intent use panrom
                Intent intent = new Intent(Welcomepage.this, LoginActivity.class);
                startActivity(intent);

                // Activity transition animation (optional but looks good)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

                btnLogin2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Login Page-ku poga Intent use panrom
                        Intent intent = new Intent(Welcomepage.this, RegisterActivity.class);
                        startActivity(intent);

                        // Activity transition animation (optional but looks good)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                }
        );



    }
}