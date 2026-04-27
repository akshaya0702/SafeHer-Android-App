package com.example.womensafety_project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);


        //button cancel codingg
        ImageView btnCancel = findViewById(R.id.btn_cancel); // Unga XML-la irukura ID
        btnCancel.setOnClickListener(v -> {
            finish(); // Idhu current profile page-ah close panni thirumba Home-ku kootitu pogum
        });

        //logout button code

        CardView cardLogout = findViewById(R.id.card_logout);
        cardLogout.setOnClickListener(v -> {
            // 1. Firebase sign out
            FirebaseAuth.getInstance().signOut();

            // 2. Welcome/Login page-ku kootitu poganum
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // History-ah clear pannum
            startActivity(intent);
            finish();

            Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
        });

        //username display
        // 1. Firebase variables
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("Users");

// 2. UI Elements
        TextView tvProfileName = findViewById(R.id.p_name);
        TextView tvProfileEmail = findViewById(R.id.p_email);

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            // 3. Database-la irundhu data edukirom
            mDatabase.child(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    String name = task.getResult().child("name").getValue(String.class);
                    String email = task.getResult().child("email").getValue(String.class);

                    tvProfileName.setText(name);
                    tvProfileEmail.setText(email);
                } else {
                    Toast.makeText(this, "Data fetch failed!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        CardView cardFriends = findViewById(R.id.card_friends);
        cardFriends.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, FriendViewActivity.class));
        });

    }
}