package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.util.NotificationHelper;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.AirQualityViewModel;

public class AirQualityRepository {

    private final DatabaseReference baseRef;
    private  SharedPreferences sharedPreferences;

    // Global notification tracking - works across all fragments
    private static boolean isGlobalAlertActive = false;
    private static long lastGlobalAlertTime = 0;
    private static final long ALERT_COOLDOWN_MS = 15 * 60 * 1000; // 15 minutes cooldown

    // Global listener for notifications (works independently of fragments)
    private ValueEventListener globalNotificationListener;
    private String currentMonitoredRoom = "Main Office";
    private Context applicationContext;

    public AirQualityRepository() {
        baseRef = FirebaseDatabase.getInstance().getReference();
        // Note: Context will be set later when needed for notifications
        sharedPreferences = null; // Will be initialized when context is available
    }

    // Initialize with context for notifications and preferences
    public void initialize(Context context) {
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
            startGlobalNotificationMonitoring();
        }
    }

    private DatabaseReference getDatabaseReference(String roomName) {
        if ("Main Office".equals(roomName)) {
            return baseRef.child("sgp30_readings");
        } else {
            return baseRef.child("rooms").child(roomName).child("air_quality_readings");
        }
    }

    public void listenForSensorData(@NonNull ValueEventListener listener, String roomName) {
        getDatabaseReference(roomName).limitToLast(1).addValueEventListener(listener);
        // Also update the global monitored room
        currentMonitoredRoom = roomName;
    }

    public void removeListener(@NonNull ValueEventListener listener, String roomName) {
        if (roomName != null) {
            getDatabaseReference(roomName).removeEventListener(listener);
        }
    }

    public Task<DataSnapshot> manualRefresh(String roomName) {
        return getDatabaseReference(roomName).limitToLast(1).get();
    }


    private void startGlobalNotificationMonitoring() {
        if (globalNotificationListener != null || applicationContext == null) return;

        globalNotificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                checkAirQualityForNotifications(dataSnapshot, currentMonitoredRoom);
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError databaseError) {

            }
        };


        getDatabaseReference(currentMonitoredRoom).limitToLast(1).addValueEventListener(globalNotificationListener);
    }


    public void checkAirQualityForNotifications(DataSnapshot dataSnapshot, String roomName) {
        if (!dataSnapshot.exists() || sharedPreferences == null) return;

        DataSnapshot latestReading = null;
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            latestReading = snapshot;
        }

        if (latestReading != null) {
            Long eco2Value = latestReading.child("eCO2").getValue(Long.class);
            if (eco2Value != null) {
                checkAlertThreshold(eco2Value.intValue(), roomName);
            }
        }
    }


    private void checkAlertThreshold(int currentValue, String roomName) {
        int alertThreshold = sharedPreferences.getInt(AirQualityViewModel.KEY_ALERT_LEVEL, 2000);

        if (currentValue > alertThreshold) {
            long now = System.currentTimeMillis();


            if (!isGlobalAlertActive || (now - lastGlobalAlertTime > ALERT_COOLDOWN_MS)) {
                String message = "High CO2 detected in " + roomName + ": " + currentValue + " PPM";

                if (applicationContext != null) {
                    NotificationHelper.sendAlert(applicationContext, roomName, message);
                }

                isGlobalAlertActive = true;
                lastGlobalAlertTime = now;
            }
        } else {
            // Reset alert state if value drops significantly below threshold
            if (currentValue < (alertThreshold - 100)) {
                isGlobalAlertActive = false;
            }
        }
    }


    public void updateGlobalMonitoredRoom(String roomName) {
        if (roomName != null && !roomName.trim().isEmpty()) {
            currentMonitoredRoom = roomName;
            // Restart global monitoring for new room
            if (globalNotificationListener != null) {
                getDatabaseReference(currentMonitoredRoom).removeEventListener(globalNotificationListener);
                getDatabaseReference(currentMonitoredRoom).limitToLast(1).addValueEventListener(globalNotificationListener);
            }
        }
    }


    public String getCurrentMonitoredRoom() {
        return currentMonitoredRoom;
    }
}
