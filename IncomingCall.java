package com.example.womensafety_project;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class IncomingCall extends AppCompatActivity {

    Ringtone ringtone;
    TextView tvName, tvStatus1;
    ImageButton btnAccept, btnReject ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        tvName = findViewById(R.id.tv_incoming_name);
        tvStatus1 = findViewById(R.id.tv_status);
        btnAccept = findViewById(R.id.btn_accept);
        btnReject = findViewById(R.id.btn_reject);


        String callerName = getIntent().getStringExtra("CALLER_NAME");
        tvName.setText(callerName);

        // Ringtone set panrom
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();

        btnAccept.setOnClickListener(v -> {
            ringtone.stop();
            tvStatus1.setText("00:01"); // Inga oru simple timer loop pottu update pannalaam
            btnAccept.setVisibility(View.GONE); // Accept button-ah maraikalam
            startTimer();
        });

        btnReject.setOnClickListener(v -> {
            ringtone.stop();
            finish(); // Screen-ah close panniduvom
        });
    }
    private int seconds = 0;
    private Handler timerHandler = new Handler();

    private void startTimer() {
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                seconds++;
                int mins = seconds / 60;
                int secs = seconds % 60;
                String time = String.format("%02d:%02d", mins, secs);
                tvStatus1.setText(time); // Status TextView-la timer odum
                timerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ringtone != null) ringtone.stop(); // App close aana ringtone nippatanum
    }
}