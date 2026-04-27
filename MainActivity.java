package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        final View topCircle = findViewById(R.id.top_circle);
        final View bottomCircle = findViewById(R.id.bottom_circle);
        final LinearLayout logoContainer = findViewById(R.id.logo_container);

        // --- STEP 1: EXPAND (0 - 1 sec) ---
        topCircle.animate().scaleX(14f).scaleY(14f).setDuration(600).setStartDelay(0);
        bottomCircle.animate().scaleX(14f).scaleY(14f).setDuration(600).setStartDelay(0);

        // --- STEP 2: STAY (1 - 2 sec) ---
        // Expansion mudinja udane 1 second wait pannum (System handle pannum automatic-ah)

        // --- STEP 3: SHRINK & REVEAL (Starts after 2 sec) ---

        // Firebase Auth check pannuvom
        // 2-3 seconds delay-ku aprom indha logic run aaganum


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Shrink circles back
                topCircle.animate().scaleX(1f).scaleY(1f)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setDuration(600);

                bottomCircle.animate().scaleX(1f).scaleY(1f)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setDuration(600);

                // Reveal Logo and Text
                logoContainer.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f)
                        .setDuration(600)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                logoContainer.animate().scaleX(1f).scaleY(1f).setDuration(200);
                            }
                        });
            }
        }, 1500); // Trigger after 2 seconds

        // FINAL STEP: Move to Next Screen (Total 3.5s for smooth finish)
        // FINAL STEP: Move to Next Screen (Total 3.5s for smooth finish)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                logoContainer.animate().alpha(0f).setDuration(300);

                bottomCircle.animate()
                        .scaleX(35f)
                        .scaleY(35f)
                        .setDuration(800)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                // --- INGA THAAN CHANGE PANNANUM ---
                                Intent intent;
                                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                                    // User already login panni irundha Home-ku ponga
                                    intent = new Intent(MainActivity.this, HomeActivity.class);
                                } else {
                                    // Login pannala na Welcomepage-ku ponga
                                    intent = new Intent(MainActivity.this, Welcomepage.class);
                                }

                                startActivity(intent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            }
                        });
            }
        }, 2500);


    }
}