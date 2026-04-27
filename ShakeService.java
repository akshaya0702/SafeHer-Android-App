package com.example.womensafety_project;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class ShakeService extends Service implements SensorEventListener {

    // FIX: Assigned a value to NOTIFICATION_ID
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "shake_service_channel";
    private static final String TAG = "ShakeService";

    private TextToSpeech tts;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long lastShakeTime = 0;
    private int shakeCount = 0;

    private String currentMode = "NORMAL";

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Sensor Management
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAccelerometer != null) {
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }

        // 2. Initialize TTS
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) {
                tts.setLanguage(Locale.US);
            }
        });

        // 3. Setup Notification and Start Foreground
        createNotificationChannel();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ID = "shake_service_channel";

        // 1. Create the Notification Channel (required for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Detection Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Safety Alert Active")
                .setContentText("Shake to send SOS alert.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        // FIX: Consolidated foreground service start logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Note: Ensure you have FOREGROUND_SERVICE_TYPE_LOCATION in Manifest if using this type
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        Toast.makeText(this, "Shake Service Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String mode = intent.getStringExtra("MODE");
            if (mode != null) {
                currentMode = mode;
            }
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Acceleration Math
        float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        // Force check - 15.0f is better for testing
        if (acceleration > 18.0f) {
            long now = System.currentTimeMillis();
            if (now - lastShakeTime < 500) { // Romba fast-ah shake pannanum
                shakeCount++;
            } else {
                shakeCount = 1; // Gap viluntha count reset
            }
            lastShakeTime = now;

            // 3 times continuous-ah shake panna thaan trigger aagum
            if (shakeCount >= 2) {
                shakeCount = 0; // Reset count

                // BLIND Mode check for voice
                if ("BLIND".equals(currentMode) && tts != null) {
                    tts.speak("Emergency shake detected. Launching SOS screen.", TextToSpeech.QUEUE_FLUSH, null, null);
                }

                Intent sosIntent = new Intent(this, SosActionActivity.class);
                sosIntent.putExtra("FROM_SHAKE", true);
                sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(sosIntent);
            }
        }
    }

    private void triggerSOS() {
        Log.d(TAG, "SOS Triggered by Shake");

        // BLIND Mode check for voice
        if ("BLIND".equals(currentMode) && tts != null) {
            tts.speak("Emergency shake detected. Launching SOS screen.", TextToSpeech.QUEUE_FLUSH, null, null);
        }

        Intent sosIntent = new Intent(this, SosActionActivity.class);
        sosIntent.putExtra("FROM_SHAKE", true);
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(sosIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SafeHer Shield Notifications",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        Toast.makeText(this, "Shake Service Stopped", Toast.LENGTH_SHORT).show();
    }
}