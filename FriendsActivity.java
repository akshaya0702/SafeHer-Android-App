package com.example.womensafety_project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class FriendsActivity extends AppCompatActivity {

    private EditText etName, etNumber;
    private Button btnSave;
    private DatabaseReference mDatabase;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // 1. Initialize Views
        etName = findViewById(R.id.et_contact_name);
        etNumber = findViewById(R.id.et_contact_number);
        btnSave = findViewById(R.id.btn_save_contact);
        ImageView btnCancel = findViewById(R.id.btn_cancel1);

        // 2. Firebase Setup with URL
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Database URL-ah inge initialize pannunga
            mDatabase = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                    .getReference("Users").child(userId).child("EmergencyContacts");
        }

        // Cancel Button logic
        btnCancel.setOnClickListener(v -> finish());

        // 3. Save Button Logic
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etNumber.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            } else if (phone.length() < 10) {
                Toast.makeText(this, "Enter valid 10 digit number", Toast.LENGTH_SHORT).show();
            } else {
                saveContactToFirebase(name, phone);
            }
        });
    } // onCreate method ends here

    private void saveContactToFirebase(String name, String phone) {
        if (mDatabase == null) {
            Toast.makeText(this, "Database connection failed!", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> contactMap = new HashMap<>();
        contactMap.put("name", name);
        contactMap.put("phone", phone);
        contactMap.put("latitude", MapActivity.myLat);
        contactMap.put("longitude", MapActivity.myLon);

        // Default Icon set panrom
        contactMap.put("iconType", "default");

        // .push() ensures multiple contacts are saved as a list
        mDatabase.push().setValue(contactMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(FriendsActivity.this, "Contact Added Successfully!", Toast.LENGTH_SHORT).show();
                finish(); // Go back to profile/previous screen
            } else {
                Toast.makeText(FriendsActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}