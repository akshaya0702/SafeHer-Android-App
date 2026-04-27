package com.example.womensafety_project;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.osmdroid.bonuspack.routing.*;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import android.os.Looper;

public class MapActivity extends AppCompatActivity {

    private MapView map = null;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    public static double myLat = 0.0;
    public static double myLon = 0.0;

    private Marker userMarker;
    private Polyline currentRoute;

    private boolean isNavigationActive = false;
    private Road currentRoadObject;

    private android.speech.tts.TextToSpeech tts;

    // --- FIX: btnStopNav initialized ---
    private Button btnStopNav;

    HashMap<String, Boolean> alertSentMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_map);

        map = findViewById(R.id.mapview);
        ImageButton btnAddMore = findViewById(R.id.btn_add_more);
        // Initialize this view

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setTilesScaledToDpi(true);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        GeoPoint startPoint = new GeoPoint(13.0827, 80.2707); // Chennai
        map.getController().setCenter(startPoint);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadBubblesFromAllSources();
        }

        btnAddMore.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, HomeActivity.class);
            intent.putExtra("OPEN_DIALOG", true);
            startActivity(intent);
            finish(); // Map closes, Home opens
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                        GeoPoint myPoint = new GeoPoint(myLat, myLon);

                        // Check safety in background
                        checkSafetyDistanceForAll();

                        if (userMarker == null) {
                            userMarker = new Marker(map);
                            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            userMarker.setIcon(getResources().getDrawable(R.drawable.outline_location_on_24));
                            map.getOverlays().add(userMarker);
                        }
                        userMarker.setPosition(myPoint);

                        if (isNavigationActive && currentRoadObject != null) {
                            map.getController().animateTo(myPoint);
                            // Instruction Logic
                            for (int i = 0; i < currentRoadObject.mNodes.size(); i++) {
                                RoadNode node = currentRoadObject.mNodes.get(i);
                                double distanceToNode = myPoint.distanceToAsDouble(node.mLocation);
                                if (distanceToNode < 30) {
                                    String instruction = node.mInstructions;
                                    if (instruction != null && !instruction.isEmpty()) {
                                        speakInstruction(instruction);
                                        node.mInstructions = ""; // Mark as used
                                    }
                                    break;
                                }
                            }
                        }
                        map.invalidate();
                    }
                }
            }
        };

        startLocationUpdates();

        tts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.setLanguage(java.util.Locale.US);
            }
        });

        if (btnStopNav != null) {
            btnStopNav.setOnClickListener(v -> {
                isNavigationActive = false;
                btnStopNav.setVisibility(View.GONE);
                if (currentRoute != null) {
                    map.getOverlays().remove(currentRoute);
                    currentRoute = null;
                }
                map.invalidate();
                speakInstruction("Navigation stopped.");
            });
        }
    }

    private void loadBubblesFromAllSources() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        LinearLayout bubbleLayout = findViewById(R.id.map_bubble_layout);
        bubbleLayout.removeAllViews();

        DatabaseReference safeRef = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("SafeBubbles");

        safeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                bubbleLayout.removeAllViews();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("name").getValue(String.class);
                    String phone = ds.child("phone").getValue(String.class);
                    String icon = ds.child("iconType").getValue(String.class);
                    Double bLat = ds.child("latitude").getValue(Double.class);
                    Double bLon = ds.child("longitude").getValue(Double.class);

                    if (bLat != null && bLon != null && myLat != 0.0) {
                        double dist = calculateDistance(myLat, myLon, bLat, bLon);
                        if (dist > 30) {
                            sendSafetyAlertNotification(name, dist);
                        }
                    }

                    Long expiry = ds.child("expiryTime").getValue(Long.class);
                    if (expiry == null) expiry = -1L;
                    if (expiry != -1 && System.currentTimeMillis() > expiry) {
                        ds.getRef().removeValue();
                        showRecallPopup(name, phone, icon);
                    } else {
                        addBubbleToMapUI(name, ds);
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        DatabaseReference emergencyRef = FirebaseDatabase.getInstance("https://womensafety-project-63cfa-default-rtdb.firebaseio.com/")
                .getReference("Users").child(userId).child("EmergencyContacts");

        emergencyRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = ds.child("name").getValue(String.class);
                    if (name != null) addBubbleToMapUI(name, ds);
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void addBubbleToMapUI(String name, DataSnapshot ds) {
        LinearLayout bubbleLayout = findViewById(R.id.map_bubble_layout);
        if (bubbleLayout == null) return;

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(25, 10, 25, 10);
        item.setGravity(Gravity.CENTER);

        Double bLat = ds.child("latitude").getValue(Double.class);
        Double bLon = ds.child("longitude").getValue(Double.class);
        String phone = ds.child("phone").getValue(String.class);
        String iconType = ds.child("iconType").getValue(String.class);

        de.hdodenhof.circleimageview.CircleImageView img = new de.hdodenhof.circleimageview.CircleImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(140, 140));

        if ("appa".equals(iconType)) img.setImageResource(R.drawable.ic_appa);
        else if ("amma".equals(iconType)) img.setImageResource(R.drawable.ic_amma);
        else img.setImageResource(R.drawable.ic_profile);

        img.setBorderColor(getResources().getColor(android.R.color.holo_purple));
        img.setBorderWidth(3);

        item.setOnLongClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, v);
            popup.getMenu().add("Edit Bubble");
            popup.getMenu().add("Delete Bubble");

            popup.setOnMenuItemClickListener(itemMenu -> {
                if (itemMenu.getTitle().equals("Edit Bubble")) {
                    Intent intent = new Intent(MapActivity.this, HomeActivity.class);
                    intent.putExtra("IS_EDIT", true);
                    intent.putExtra("NAME", ds.child("name").getValue(String.class));
                    intent.putExtra("PHONE", ds.child("phone").getValue(String.class));
                    intent.putExtra("BUBBLE_ID", ds.getKey());
                    intent.putExtra("ICON_TYPE", ds.child("iconType").getValue(String.class));
                    startActivity(intent);
                    finish();
                } else if (itemMenu.getTitle().equals("Delete Bubble")) {
                    ds.getRef().removeValue();
                }
                return true;
            });
            popup.show();
            return true;
        });

        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextSize(12);
        tv.setTextColor(getResources().getColor(android.R.color.black));

        item.addView(img);
        item.addView(tv);
        bubbleLayout.addView(item);

        item.setOnClickListener(v -> {
            if (bLat != null && bLon != null) {
                double dist = calculateDistance(myLat, myLon, bLat, bLon);
                GeoPoint target = new GeoPoint(bLat, bLon);
                map.getController().animateTo(target);

                // Clear and add specific marker
                map.getOverlays().removeIf(o -> o instanceof Marker && o != userMarker);
                Marker m = new Marker(map);
                m.setPosition(target);
                m.setTitle(name);
                map.getOverlays().add(m);
                map.invalidate();
                showRouteSheet(name, phone, bLat, bLon, dist);
            }
        });
    }

    private double calculateDistance(double userLat, double userLon, double bubbleLat, double bubbleLon) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLon, bubbleLat, bubbleLon, results);
        return results[0] / 1000;
    }

    private void showRecallPopup(String name, String phone, String icon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_recall_bubble, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvMsg = view.findViewById(R.id.tv_recall_message);
        tvMsg.setText(name + "'s temporary time is over. Do you want to keep them permanently?");

        Button btnPermanent = view.findViewById(R.id.btn_make_permanent);
        Button btnDismiss = view.findViewById(R.id.btn_dismiss_recall);

        btnPermanent.setOnClickListener(v -> {
            savePermanentBubble(name, phone, icon);
            dialog.dismiss();
        });
        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void savePermanentBubble(String name, String phone, String icon) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users")
                .child(userId).child("SafeBubbles").push();

        HashMap<String, Object> mapData = new HashMap<>();
        mapData.put("name", name);
        mapData.put("phone", phone);
        mapData.put("iconType", icon);
        mapData.put("expiryTime", -1);
        mapData.put("latitude", myLat);
        mapData.put("longitude", myLon);

        // --- FIX: Used 'mapData' instead of undefined 'map' ---
        ref.setValue(mapData).addOnSuccessListener(aVoid ->
                Toast.makeText(this, name + " is now Permanent!", Toast.LENGTH_SHORT).show()
        );
    }

    private void speakInstruction(String text) {
        if (tts != null && isNavigationActive && text != null && !text.isEmpty()) {
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    private void showRouteSheet(String name, String phone, double destLat, double destLon, double dist) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_route, null);

        TextView tvName = v.findViewById(R.id.tv_dest_name);
        TextView tvDist = v.findViewById(R.id.tv_distance);
        TextView tvAddress = v.findViewById(R.id.tv_address);
        Button btnStart = v.findViewById(R.id.btn_start_navigation);
        Button btnStop = v.findViewById(R.id.btn_stop_navigation);

        tvName.setText(name);
        tvDist.setText("Distance: " + String.format("%.2f", dist) + " km");

        new Thread(() -> {
            String addr = getAddressFromLocation(destLat, destLon);
            runOnUiThread(() -> tvAddress.setText(addr));
        }).start();

        if (isNavigationActive) {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
        }

        v.findViewById(R.id.btn_mode_car).setOnClickListener(view -> {
            drawRouteOnMap(new GeoPoint(myLat, myLon), new GeoPoint(destLat, destLon), "car", tvDist);
            bottomSheet.dismiss();
        });

        btnStart.setOnClickListener(view -> {
            isNavigationActive = true;
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            map.getController().setZoom(18.0);
            if (currentRoadObject != null && !currentRoadObject.mNodes.isEmpty()) {
                speakInstruction("Starting navigation. " + currentRoadObject.mNodes.get(0).mInstructions);
            }
            bottomSheet.dismiss();
        });

        btnStop.setOnClickListener(view -> {
            isNavigationActive = false;
            btnStop.setVisibility(View.GONE);
            btnStart.setVisibility(View.VISIBLE);
            if (currentRoute != null) map.getOverlays().remove(currentRoute);
            map.invalidate();
            bottomSheet.dismiss();
        });

        v.findViewById(R.id.btn_call_now).setOnClickListener(view -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
            startActivity(callIntent);
        });

        bottomSheet.setContentView(v);
        bottomSheet.show();
    }

    private void drawRouteOnMap(GeoPoint start, GeoPoint end, String mode, TextView tvTime) {
        new Thread(() -> {
            OSRMRoadManager roadManager = new OSRMRoadManager(this, "SafeHer");
            if (mode.equals("bike")) roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE);
            else roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR);

            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(start);
            waypoints.add(end);

            try {
                Road road = roadManager.getRoad(waypoints);
                Polyline roadOverlay = RoadManager.buildRoadOverlay(road);

                if (road.mDuration / 60 > 30) roadOverlay.getOutlinePaint().setColor(Color.RED);
                else roadOverlay.getOutlinePaint().setColor(Color.GREEN);
                roadOverlay.getOutlinePaint().setStrokeWidth(12.0f);

                runOnUiThread(() -> {
                    currentRoadObject = road;
                    map.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
                    if (currentRoute != null) map.getOverlays().remove(currentRoute);
                    currentRoute = roadOverlay;
                    map.getOverlays().add(currentRoute);

                    int duration = (int) (road.mDuration / 60);
                    tvTime.setText(String.format("%.1f km • %d min", road.mLength, duration));
                    map.invalidate();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Route generation failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    private void sendSafetyAlertNotification(String name, double distance) {
        String channelId = "safety_alerts";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Safety Alerts", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.outline_location_on_24)
                .setContentTitle("🚨 Safety Alert!")
                .setContentText(name + " is " + String.format("%.1f", distance) + " km away!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(101, builder.build());
    }

    private String getAddressFromLocation(double lat, double lon) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            return "Address fetch failed";
        }
        return "Unknown Location";
    }

    private void checkSafetyDistanceForAll() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users").child(userId).child("SafeBubbles");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Double bLat = ds.child("latitude").getValue(Double.class);
                    Double bLon = ds.child("longitude").getValue(Double.class);
                    String name = ds.child("name").getValue(String.class);

                    if (bLat != null && bLon != null && myLat != 0.0) {
                        double dist = calculateDistance(myLat, myLon, bLat, bLon);
                        if (dist > 30.0) {
                            if (!alertSentMap.containsKey(name) || !alertSentMap.get(name)) {
                                sendSafetyAlertNotification(name, dist);
                                alertSentMap.put(name, true);
                            }
                        } else {
                            alertSentMap.put(name, false);
                        }
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}