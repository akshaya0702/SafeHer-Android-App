package com.example.womensafety_project;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SosActionActivity extends AppCompatActivity {
    private TextView tvCountdown, tvContactName;
    private int timeLeft = 5;
    private Handler handler = new Handler();
    private boolean isCancelled = false;
    private String currentUserName = "User";
    private final String dbURL = "https://womensafety-project-63cfa-default-rtdb.firebaseio.com/";
    private boolean isFromShake = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos_action);

        tvCountdown = findViewById(R.id.tv_countdown);
        tvContactName = findViewById(R.id.tv_contact_info);
        Button btnCancel = findViewById(R.id.btn_cancel_sos);
        // onCreate-kulla poodunga
        isFromShake = getIntent().getBooleanExtra("FROM_SHAKE", false);

        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance(dbURL).getReference("Users").child(userId);

        userRef.child("name").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                currentUserName = snapshot.getValue(String.class);
            }
        });

        userRef.child("EmergencyContacts").get().addOnSuccessListener(dataSnapshot -> {
            if (dataSnapshot.exists()) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String name = ds.child("name").getValue(String.class);
                    tvContactName.setText("Alerting: " + name);
                    break;
                }
            }
        });

        startCountdown();
        btnCancel.setOnClickListener(v -> {
            isCancelled = true;
            finish();
        });
    }

    private void startCountdown() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCancelled) return;
                if (timeLeft > 0) {
                    tvCountdown.setText(String.valueOf(timeLeft));
                    timeLeft--;
                    handler.postDelayed(this, 1000);
                } else {
                    sendSOSFinal();
                }
            }
        }, 1000);
    }

    // SosActionActivity.java-la indha method-ah check pannunga
    private void sendSOSFinal() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        // Context check for safety
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> {
                        String currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                        String name = (currentUserName != null) ? currentUserName : "User";
                        String message;

                        if (location != null) {
                            double lat = location.getLatitude();
                            double lon = location.getLongitude();
                            String mapLink = "http://maps.google.com/maps?q=" + lat + "," + lon; // Corrected Map Link format

                             message = "🚨 EMERGENCY ALERT 🚨\n" +
                                    currentUserName + " may be in danger.\n\n" +
                                    "Emotion detected: PANIC\n" +
                                    "Map Link: " + mapLink + "\n" +
                                    "Time: " + currentTime + "\n\n" +
                                    "Please contact immediately.";


                        } else {
                            // Location kidaikkalanaalum name and time anuppurom
                            message = "🚨 EMERGENCY ALERT 🚨\n" + name + " is in danger!\n" +
                                    "Time: " + currentTime + "\n" +
                                    "(GPS signal weak, last known location unavailable)";
                        }
                        fetchContactsAndSend(message);
                    })
                    .addOnFailureListener(e -> {
                        fetchContactsAndSend("🚨 EMERGENCY ALERT 🚨\n" + currentUserName + " is in danger! Please check immediately.");
                    });
        } else {
            fetchContactsAndSend("🚨 EMERGENCY ALERT 🚨\n" + currentUserName + " is in danger! (Location Permission Denied)");
        }
    }

    private void fetchContactsAndSend(String message) {
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance(dbURL).getReference("Users").child(userId).child("EmergencyContacts");

        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                SmsManager smsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    smsManager = getSystemService(SmsManager.class);
                } else {
                    smsManager = SmsManager.getDefault();
                }

                for (DataSnapshot ds : task.getResult().getChildren()) {
                    String phone = ds.child("phone").getValue(String.class);
                    if (phone != null && !phone.isEmpty()) {
                        try {
                            ArrayList<String> parts = smsManager.divideMessage(message);
                            if (isFromShake) {
                                // SHAKE-naa OREY ORU dharava mattum anuppum
                                smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                            } else {
                                for (int i = 0; i < 5; i++) {
                                    smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                                    // Oru chinna delay (optional) - 1 second gap between messages
                                    Thread.sleep(1000);
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                Toast.makeText(this, "SOS Sent Successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}