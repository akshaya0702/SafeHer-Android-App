package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class ModeSelectionActivity extends AppCompatActivity {
    private TextToSpeech tts;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mode_selection);

        Button btnNormal = findViewById(R.id.btn_normal_mode);
        Button btnBlind = findViewById(R.id.btn_blind_mode);

        // Initialize Voice Feedback (TTS)
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
                // Welcome message for Blind users
                tts.speak("Welcome to SafeHer. Please select a mode. Top button for Normal mode, Bottom button for Blind mode.", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        btnNormal.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("MODE", "NORMAL");
            startActivity(intent);
        });

        btnBlind.setOnClickListener(v -> {
            tts.speak("Blind Mode Activated. Hands-free monitoring is now on.", TextToSpeech.QUEUE_FLUSH, null, null);
            Intent intent = new Intent(this, HomeActivity.class);
            intent.putExtra("MODE", "BLIND");
            startActivity(intent);
        });
    }


}
