package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FakeCallActivity extends AppCompatActivity {

    EditText etCallerName;
    Button btn10Sec, btn30Sec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_call);

        etCallerName = findViewById(R.id.et_caller_name);
        btn10Sec = findViewById(R.id.btn_10sec);
        btn30Sec = findViewById(R.id.btn_30sec);

        btn10Sec.setOnClickListener(v -> startFakeCall(10000)); // 10 seconds
        btn30Sec.setOnClickListener(v -> startFakeCall(30000)); // 30 seconds

        ImageView image = findViewById(R.id.iv_profile);

        image.setOnClickListener(v ->
        {
            finish();
            Intent intent = new Intent(this, HomeActivity.class);
        });

    }

    private void startFakeCall(int delay) {
        String name = etCallerName.getText().toString();
        if (name.isEmpty()) name = "Dad"; // Default name

        String finalName = name;
        Toast.makeText(this, "Fake call scheduled in " + (delay/1000) + " seconds", Toast.LENGTH_SHORT).show();

        // Background-la timer run panna 'Handler' use panrom
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(FakeCallActivity.this, IncomingCall.class);
            intent.putExtra("CALLER_NAME", finalName);
            startActivity(intent);
        }, delay);

        // App-ah minimize panni user-ah safe-ah feel panna vekkalaam
        moveTaskToBack(true);

    }
}