package com.example.womensafety_project;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class EmotionResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_result);

        // UI elements connect panrom
        ImageView imgEmoji = findViewById(R.id.img_emotion);
        TextView tvTitle = findViewById(R.id.tv_fear_status);
        TextView tvSupportText = findViewById(R.id.tv_support_message);
        View rootLayout = findViewById(R.id.root_layout);
        Button btnYes = findViewById(R.id.btn_send_sos);
        Button btnNo = findViewById(R.id.btn_im_safe);
        Button btnCancel = findViewById(R.id.btn_cancel_alert);

        // HomeActivity-la irundhu vara emotion data
        String emotion = getIntent().getStringExtra("EMOTION_TYPE");

        if (emotion == null) emotion = "NORMAL";

        // DYNAMIC UI LOGIC
        switch (emotion) {
            case "SAD":
                tvTitle.setText("SADNESS DETECTED");
                tvTitle.setTextColor(Color.parseColor("#1976D2")); // Blue
                imgEmoji.setImageResource(R.drawable.emoji_sad); // Unga sad emoji
                tvSupportText.setText("You seem sad today.\nWould you like to talk to our AI chatbot?");
                btnYes.setText("TALK TO CHATBOT");
                btnNo.setText("I'M OKAY");
                rootLayout.setBackgroundColor(Color.parseColor("#E3F2FD")); // Light Blue
                break;

            case "FEAR":
                tvTitle.setText("FEAR DETECTED");
                tvTitle.setTextColor(Color.parseColor("#FBC02D")); // Yellow/Gold
                imgEmoji.setImageResource(R.drawable.emoji_fear);
                tvSupportText.setText("We detected signs of fear.\nAre you feeling unsafe right now?");
                btnYes.setText("YES - SEND SOS");
                btnNo.setText("NO - I'M SAFE");
                rootLayout.setBackgroundColor(Color.parseColor("#FFFDE7")); // Light Yellow
                break;

            case "PANIC":
                tvTitle.setText("EXTREME PANIC!");
                tvTitle.setTextColor(Color.parseColor("#D32F2F")); // Red
                imgEmoji.setImageResource(R.drawable.emoji_panic);
                tvSupportText.setText("Severe distress detected!\nSending emergency alert in 3...2...1...");
                btnYes.setText("SEND SOS NOW");
                btnNo.setText("CANCEL ALERT");
                rootLayout.setBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
                // Inga venumna 3 sec timer start pannalaam
                break;

            default: // NORMAL
                tvTitle.setText("NORMAL");
                tvSupportText.setText("Everything looks normal. Stay safe!");
                break;
        }


        // NO thotta back poyiduvom
        btnNo.setOnClickListener(v -> finish());

        btnCancel.setOnClickListener(v -> finish());
        // EmotionResultActivity.java kulla idhai add pannunga


        btnNo.setOnClickListener(v -> {
            finish(); // Page-ah close panni thirumba monitoring-ku poga
        });

        String finalEmotion = emotion;
        btnYes.setOnClickListener(v -> {
            if (finalEmotion.equals("SAD")) {
                // Chatbot page-ku poga (Oru Toast potruken, neenga chatbot intent kudukalam)
                Toast.makeText(this, "Opening AI Chatbot Support...", Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(this, ChatBotActivity.class);
                // startActivity(intent);
            } else {
                // Fear or Panic-na mattum SOS anupuvom
                Toast.makeText(this, "Emergency SOS Sent to Contacts!", Toast.LENGTH_LONG).show();
                // sendSOS(); // Unga message logic
            }
            finish();
        });
    }
}