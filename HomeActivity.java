package com.example.womensafety_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private String currentMode = "NORMAL"; // Default
    private TextToSpeech tts;

    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private long lastClickTime = 0; // Class level-la irukanum
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;

    private boolean isSosTriggered = false; // Add this line

    private int panicFrameCount = 0;
    private long panicStartTime = 0;
    private int currentLevel = 0;

    private FloatingActionButton fabAddBubble;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        ImageView imgProfile = findViewById(R.id.img_profile);
        FloatingActionButton fabSos = findViewById(R.id.fab_sos);
        TextView tvPrivacy = findViewById(R.id.tv_privacy_policy);



        // SOS Button Logic
        fabSos.setOnClickListener(v -> {
            if ("BLIND".equals(currentMode)) {
                speak("SOS button pressed. Starting five second countdown.");
            }
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                // Double Tap: Direct SOS
                if (checkPermissions()) {
                    startShakeService();
                    Toast.makeText(this, "Double Tap! Sending SOS...", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissions();
                }
            } else {
                // Single Tap: Open Countdown Page
                handleSingleTap();
            }
            lastClickTime = clickTime;
        });

        // Profile Click Logic (Method kulla kondu vandhutten)
        if (imgProfile != null) {
            imgProfile.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
        ImageButton cardFriends = findViewById(R.id.img_call);
        cardFriends.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FriendsActivity.class));
        });

        ImageButton cardchatbot = findViewById(R.id.img_chatbot);
        cardchatbot.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FakeCallActivity.class));
        });

        ImageButton faceid = findViewById(R.id.img_face);
        faceid.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, EmotionResultActivity.class));
        });


        FloatingActionButton fabChat = findViewById(R.id.fab_chatbot);
        fabChat.setOnClickListener(view -> {
            Intent intent = new Intent(this, ChatbotActivity.class);
            startActivity(intent);
        });

        // Logic to handle Shake Switch
        SwitchMaterial switchShake = findViewById(R.id.switch_shake);

// 1. Check if service is already running and set switch state
// SharedPreferences use panna innum professional-ah irukkum, but for now:
        switchShake.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent shakeIntent = new Intent(this, ShakeService.class);
            if (isChecked) {
                // Switch ON panna mattumae service start aaganum
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(shakeIntent);
                } else {
                    startService(shakeIntent);
                }
                Toast.makeText(this, "Shake Protection Activated", Toast.LENGTH_SHORT).show();
            } else {
                // Switch OFF panna service-ah total-ah stop panniduvom
                stopService(shakeIntent);
                Toast.makeText(this, "Shake Protection Deactivated", Toast.LENGTH_SHORT).show();
            }
        });

// Privacy Policy link logic

        tvPrivacy.setOnClickListener(v -> {
            // Unga privacy policy URL inga poodunga
            String url = "https://sites.google.com/view/safeher-app-policy/home";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });



        CardView cameraCard = findViewById(R.id.camera_window);
        cameraCard.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCameraWithAnalysis();
                Toast.makeText(this, "AI Monitoring Started", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            }
        });

        currentMode = getIntent().getStringExtra("MODE");

        if ("BLIND".equals(currentMode)) {
            // Blind mode-na automatic-ah camera start aidunum
            startCameraWithAnalysis();
            setupBlindModeTTS();
        }

        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);

                // Mode check panni welcome message sollanum
                if ("BLIND".equals(currentMode)) {
                    speak("Blind mode active. I am monitoring your face and phone movement. Don't worry, you are safe with SafeHer.");
                }
            }
        });

        if ("BLIND".equals(currentMode)) {
            setupVoiceCommand();
        }


        // Views-ah initialize pannunga
        // 1. Views-ah link panrom
        Button btnCreate = findViewById(R.id.btn_trigger_bubble);


// 2. Button Click Logic
        btnCreate.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("SafeHerPrefs", MODE_PRIVATE);
            boolean isBubbleCreated = prefs.getBoolean("isBubbleCreated", false);

            if (isBubbleCreated) {
                // Direct-ah MapActivity-ku pogum
                Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                startActivity(intent);
            } else {
                // Modhal dharava panna mattum Dialog open aagum
                showCreateBubbleDialog();
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 123);
            }
        }


        // HomeActivity.java -> onCreate-oda last-la
        if (getIntent().getBooleanExtra("OPEN_DIALOG", false)) {
            // Plus button click panni vandha, direct-ah dialog-ah open panrom
            showCreateBubbleDialog();
        }

        if (getIntent().getBooleanExtra("IS_EDIT", false)) {
            String oldName = getIntent().getStringExtra("NAME");
            String oldPhone = getIntent().getStringExtra("PHONE");
            String bId = getIntent().getStringExtra("BUBBLE_ID");
            String oldIcon = getIntent().getStringExtra("ICON_TYPE");

            // Dialog-ah open panni data-ah fill panrom
            showEditBubbleDialog(oldName, oldPhone, bId, oldIcon);
        }

        // HomeActivity-la onCreate-la idhai try pannunga
        



    }

    private void setupBlindModeTTS() {
    }
    private void setupVoiceCommand() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            return; // Permission illana exit
        }


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    // Indha line method-kulla irukkannu paarunga
                    Toast.makeText(HomeActivity.this, "I heard: " + matches.get(0), Toast.LENGTH_SHORT).show();
                    for (String result : matches) {
                        String voice = result.toLowerCase();
                        Toast.makeText(HomeActivity.this, "Heard: " + voice, Toast.LENGTH_SHORT).show();
                        // 1. SOS Trigger (Always Active)
                        if (voice.contains("help") || voice.contains("sos")) {
                            triggerSOS();
                            break;
                        }
                        // 2. STOP logic
                        else if (voice.contains("stop")) {
                            isSosTriggered = true;
                            speak("Monitoring paused. Say start to resume.");
                            try {
                                ProcessCameraProvider.getInstance(HomeActivity.this).get().unbindAll();
                            } catch (Exception e) { e.printStackTrace(); }
                            break; // Switch switch command process aana loop-ah vittu veliya vandhuru
                        }
                        // 3. START logic
                        else if (voice.contains("start")) {
                            isSosTriggered = false;
                            speak("Monitoring restarted. Camera active.");
                            startCameraWithAnalysis();
                            break;
                        }
                    }
                }
                // ROMBA MUKKIYAM: Command edhuva irundhalum thirumba listen panna aarambikkanum
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    speechRecognizer.startListening(speechIntent);
                }, 1000);
            }
            @Override
            public void onRmsChanged(float rmsdB) {
            }
            @Override
            public void onBeginningOfSpeech() {
            }
            @Override
            public void onBufferReceived(byte[] buffer) {
            }
            @Override
            public void onEndOfSpeech() {
            }
            @Override
            public void onError(int error) {
                // Error vandha thirumba start pannanum
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    speechRecognizer.startListening(speechIntent);
                }, 1000);
            }
            @Override
            public void onEvent(int eventType, Bundle params) {
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
            }
            @Override
            public void onReadyForSpeech(Bundle params) {
            }            // ... matha override methods dummy-ah pottukonga ...
        });
        speechRecognizer.startListening(speechIntent);
    }
    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.RECORD_AUDIO},  1);
            return false; // Return statement must be here
        }
    }
    private void startShakeService() {
        // Inside Shake Detection logic
        if ("BLIND".equals(currentMode)) {
            speak("Shake detected. Triggering emergency SOS.");
        }
        Intent intent = new Intent(this, ShakeService.class);
        // Crash prevent panna startForegroundService use pannanum
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, PERMISSION_REQUEST_CODE);
    }
    private void handleSingleTap() {
        Intent intent = new Intent(HomeActivity.this, SosActionActivity.class);
        startActivity(intent);
    }
    public void sendSOS() { // Public-ah maathuna dhaan vera activity-la irundhu call panna mudiyum
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                double latitude = 0.0, longitude = 0.0;
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
                String mapLink = "https://www.google.com/maps?q=" + latitude + "," + longitude;
                String message = "EMERGENCY! I am in trouble. My Location: " + mapLink;
                fetchContactAndSendSMS(message);
                // Inside sendSOS() success logic
                speak("SOS messages have been sent successfully. Help is on the way.");
            });
        }
    }
    private void fetchContactAndSendSMS(String message) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // 1. Unga real database URL-ah inga paste pannunga
        DatabaseReference ref = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("EmergencyContacts");
        ref.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // 2. DataSnapshot loop - Idhu ovvoru contact-ayum edukkum
                for (com.google.firebase.database.DataSnapshot ds : task.getResult().getChildren()) {
                    String phone = ds.child("phone").getValue(String.class);
                    String name = ds.child("name").getValue(String.class);

                    if (phone != null && !phone.isEmpty()) {
                        try {
                            // 3. Modern SmsManager logic (Recommended for newer Android)
                            android.telephony.SmsManager smsManager;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                smsManager = this.getSystemService(android.telephony.SmsManager.class);
                            } else {
                                smsManager = android.telephony.SmsManager.getDefault();
                            }

                            smsManager.sendTextMessage(phone, null, message, null, null);
                            Toast.makeText(this, "SOS Sent to " + name, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed for " + name + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else {
                Toast.makeText(this, "No Contacts Found! Please add friends first.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendSOS();
        }
    }    // Panic Logic: Smiling probability low-ah irukkaணும் & Eyes wide open-ah irukkaணும்

    private void startCameraWithAnalysis() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Preview View Setup
                Preview preview = new Preview.Builder().build();
                PreviewView previewView = new PreviewView(this);
                FrameLayout container = findViewById(R.id.camera_window);
                container.removeAllViews();
                container.addView(previewView);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. Face Detector Options (Optimized for Panic Detection)
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Indha line dhaan smile/eyes prob tharum
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                // 3. Image Analysis (The Brain)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
                    if (isSosTriggered) {
                        imageProxy.close();
                        return;
                    }

                    @SuppressLint("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        detector.process(image)
                                .addOnSuccessListener(faces -> {
                                    for (Face face : faces) {
                                        float smile = face.getSmilingProbability() != null ? face.getSmilingProbability() : 1f;
                                        float leftEye = face.getLeftEyeOpenProbability() != null ? face.getLeftEyeOpenProbability() : 0.5f;
                                        float rightEye = face.getRightEyeOpenProbability() != null ? face.getRightEyeOpenProbability() : 0.5f;

                                        String emotion = "NORMAL";

                                        // LEVEL 4: EXTREME PANIC (Eyes wide + Shake + No smile)
                                        if (smile < 0.05f && leftEye > 0.98f && rightEye > 0.98f) {
                                            if (panicStartTime == 0)
                                                panicStartTime = System.currentTimeMillis();

                                            // 5 seconds continuous check
                                            if (System.currentTimeMillis() - panicStartTime > 5000) {
                                                navigateToEmotionPage("PANIC");
                                            }
                                        }
                                        // LEVEL 3: FEAR (Eyes open + No smile)
                                        else if (smile < 0.15f && leftEye > 0.90f && rightEye > 0.90f) {
                                            navigateToEmotionPage("FEAR");
                                        }
                                        // LEVEL 2: SAD (No smile + Eyes normal)
                                        else if (smile < 0.10f) {
                                            navigateToEmotionPage("SAD");
                                        } else {
                                            panicStartTime = 0; // Reset
                                        }
                                    }
                                });

                    }
                });
                // ... (Bind to lifecycle same dhaan) ...
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Toast.makeText(this, "AI Analysis Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void triggerSOS() {
        triggerVibration();
        if ("BLIND".equals(currentMode)) {
            // Blind mode: Direct action + Voice alert
            tts.speak("Panic detected! Sending emergency alerts to your contacts now.", TextToSpeech.QUEUE_FLUSH, null, null);
            sendSOS(); // Direct SMS function
        } else {
            // Normal mode: Standard countdown screen
            Intent intent = new Intent(HomeActivity.this, SosActionActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isSosTriggered = false; // Activity stop aagumbodhu reset pannalaam
    }

    private void triggerVibration() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                // Pazhaya phones-ukku
                v.vibrate(500);
            }
        }
    }


    private void navigateToEmotionPage(String emotion) {
        // 1. Speak the emotion
        speak("Detected " + emotion + ". Opening analysis result.");

        // 2. Open the dynamic result page
        Intent intent = new Intent(HomeActivity.this, EmotionResultActivity.class);
        intent.putExtra("EMOTION_TYPE", emotion);
        startActivity(intent);

        // MUKKIYAM: Indha line dhaan thirumba detection-ah allow pannum (Home-ku thirumba varumbodhu)
        // Namma idhai onResume-layum reset pannalaam.
    }

    // triggerSOS-ah ippadi mathunga (Only for PANIC or direct triggers)


    @Override
    protected void onResume() {
        super.onResume();
        // User emotion page-la irundhu thirumba home-ku vandha, trigger-ah reset panrom
        isSosTriggered = false;
        panicStartTime = 0; // Timer-ayum reset panrom
    }


    private void showCreateBubbleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_create_bubble, null);
        builder.setView(view);


        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Rounded corner theriya

        EditText etName = view.findViewById(R.id.et_bubble_name);
        EditText etPhone = view.findViewById(R.id.et_bubble_phone);
        Button btnSave = view.findViewById(R.id.btn_save_bubble);
        ImageView imgSelected = view.findViewById(R.id.img_selected_profile);

        RadioGroup rgType = view.findViewById(R.id.rg_type);
        LinearLayout layoutDuration = view.findViewById(R.id.layout_duration);
        RadioButton rbTemporary = view.findViewById(R.id.rb_temporary);
        RadioButton rb1hr = view.findViewById(R.id.rb_1hr);


        final String[] selectedIconType = {"profile"}; // String-ah mathunga

        view.findViewById(R.id.icon_appa).setOnClickListener(v -> {
            selectedIconType[0] = "appa"; // String value set panrom
            imgSelected.setImageResource(R.drawable.ic_appa);
        });

        view.findViewById(R.id.icon_amma).setOnClickListener(v -> {
            selectedIconType[0] = "amma"; // String value set panrom
            imgSelected.setImageResource(R.drawable.ic_amma);
        });

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_temporary) {
                layoutDuration.setVisibility(View.VISIBLE);
            } else {
                layoutDuration.setVisibility(View.GONE);
            }
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString();
            String phone = etPhone.getText().toString();

            if(!name.isEmpty() && !phone.isEmpty()) {
                long currentTime = System.currentTimeMillis();
                long expiryTime = -1; // Default Permanent

                if (rbTemporary.isChecked()) {
                    if (rb1hr.isChecked()) {
                        expiryTime = currentTime + (3600 * 1000); // 1 Hour
                    } else {
                        expiryTime = currentTime + (6 * 3600 * 1000); // 6 Hours
                    }
                }

                saveToFirebase(name, phone, selectedIconType[0], expiryTime);
                dialog.dismiss();
                // Success-ana udane Map page-ku poga
                Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                startActivity(intent);
            }
            else {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveToFirebase(String name, String phone, String iconType,long expiryTime) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("SafeBubbles");

        String id = ref.push().getKey();
        HashMap<String, Object> map = new HashMap<>();
        map.put("id", id); // Edit/Delete-ku id romba mukkiyama thevai
        map.put("name", name);
        map.put("phone", phone);
        map.put("iconType", iconType);
        map.put("expiryTime", expiryTime);// "appa", "amma", "papa" nu save pannunga

        if (id != null) {
            ref.child(id).setValue(map).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Bubble Saved!", Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = getSharedPreferences("SafeHerPrefs", MODE_PRIVATE).edit();
                editor.putBoolean("isBubbleCreated", true);
                editor.apply();

                Intent intent = new Intent(HomeActivity.this, MapActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showEditBubbleDialog(String oldName, String oldPhone, String bId, String oldIcon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_create_bubble, null);
        EditText etName = view.findViewById(R.id.et_bubble_name);
        EditText etPhone = view.findViewById(R.id.et_bubble_phone);
        CircleImageView imgPreview = view.findViewById(R.id.img_selected_profile);
        Button btnSave = view.findViewById(R.id.btn_save_bubble);
        // AUTO-FILL:
        etName.setText(oldName);
        etPhone.setText(oldPhone);
        final String[] currentSelectedIcon = {oldIcon}; // Munnadi enna icon irundhucho adhu starting-la irukkum

        // Set old icon to preview
        if("appa".equals(oldIcon)) imgPreview.setImageResource(R.drawable.ic_appa);
        else if("amma".equals(oldIcon)) imgPreview.setImageResource(R.drawable.ic_amma);

        // Icon Selection
        view.findViewById(R.id.icon_appa).setOnClickListener(v -> {
            currentSelectedIcon[0] = "appa";
            imgPreview.setImageResource(R.drawable.ic_appa);
        });
        view.findViewById(R.id.icon_amma).setOnClickListener(v -> {
            currentSelectedIcon[0] = "amma";
            imgPreview.setImageResource(R.drawable.ic_amma);
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString();
            String newPhone = etPhone.getText().toString();
            // Call the update method we created above
            updateFirebaseBubble(bId, newName, newPhone, currentSelectedIcon[0]);
        });

        builder.setView(view);
        builder.show();
    }

    private void updateFirebaseBubble(String bId, String name, String phone, String iconType) {
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("SafeBubbles").child(bId);

        HashMap<String, Object> updateMap = new HashMap<>();
        updateMap.put("name", name);
        updateMap.put("phone", phone);
        updateMap.put("iconType", iconType);

        ref.updateChildren(updateMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Bubble Updated!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MapActivity.class));
            finish();
        });
    }
}