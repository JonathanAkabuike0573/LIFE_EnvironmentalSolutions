package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PresenceRepository {

    public interface TimerCallback {
        void onTimerEvent(String message);
    }

    private final DatabaseReference presenceRef;
    private String currentRoom;
    private final LightRepository lightRepository = new LightRepository();
    private final android.os.Handler vacancyHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable vacancyTimeoutRunnable;

    private boolean lastPresenceStatus = true;
    private boolean isLightOn = false;
    private boolean autoLightsOffActive = false;
    private int currentTimeoutMinutes = 5;

    private ValueEventListener autoOffListener;
    private ValueEventListener lightStatusListener;
    private TimerCallback timerCallback;

    public PresenceRepository() {
        presenceRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
    }

    public void setTimerCallback(TimerCallback callback) {
        this.timerCallback = callback;
    }


    public void startListening(@NonNull ValueEventListener listener, String roomName) {
        currentRoom = roomName;
        getRoomRef(roomName).limitToLast(1).addValueEventListener(listener);
    }

    public void stopListening(@NonNull ValueEventListener listener, String roomName) {
        getRoomRef(roomName).removeEventListener(listener);
    }

    public Task<Void> manualOverride(String status) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> firebaseData = new HashMap<>();
        firebaseData.put("room_status", status.toLowerCase());

        firebaseData.put("manual_override", true);
        firebaseData.put("timestamp", timestamp);
        return presenceRef.push().setValue(firebaseData);
    }



    public void setAutoLightsOff(String roomName, boolean enabled, int timeoutMinutes) {
        autoLightsOffActive = enabled;
        currentTimeoutMinutes = timeoutMinutes;
        this.currentRoom = roomName;


        if (autoOffListener != null) {
            getRoomRef(roomName).removeEventListener(autoOffListener);
            autoOffListener = null;
        }
        if (lightStatusListener != null) {
            getLightRef(roomName).removeEventListener(lightStatusListener);
            lightStatusListener = null;
        }

        if (!enabled) {
            cancelVacancyTimer();
            return;
        }


        lightStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean power = snapshot.child("powerOn").getValue(Boolean.class);
                boolean newLightState = (power != null && power);

                if (isLightOn != newLightState) {
                    isLightOn = newLightState;

                    if (!isLightOn) {

                        cancelVacancyTimer();
                    } else if (!lastPresenceStatus) {

                        startVacancyTimer(currentTimeoutMinutes);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        getLightRef(roomName).addValueEventListener(lightStatusListener);


        autoOffListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot latest = null;
                    for (DataSnapshot child : snapshot.getChildren()) latest = child;

                    if (latest != null) {
                        String roomStatus = latest.child("room_status").getValue(String.class);
                        boolean isOccupied = "occupied".equalsIgnoreCase(roomStatus);

                        if (isOccupied != lastPresenceStatus) {
                            lastPresenceStatus = isOccupied;

                            if (!isOccupied) {

                                if (isLightOn) {
                                    startVacancyTimer(currentTimeoutMinutes);
                                } else {

                                    Log.d("PresenceRepo", "Room vacant, but light is OFF. No action.");
                                }
                            } else {

                                cancelVacancyTimer();
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        getRoomRef(roomName).limitToLast(1).addValueEventListener(autoOffListener);
    }

    private void startVacancyTimer(int minutes) {
        cancelVacancyTimer();

        if (timerCallback != null) {
            timerCallback.onTimerEvent("Vacancy detected: Lights off in " + minutes + " min");
        }

        vacancyTimeoutRunnable = () -> {
            if (isLightOn) {
                Log.d("PresenceRepo", "Timeout reached: Turning Lights OFF");


                lightRepository.setPowerOn(currentRoom, false);

                if (timerCallback != null) {
                    timerCallback.onTimerEvent("Timeout reached: Lights turned OFF");
                }
            }
            vacancyTimeoutRunnable = null;
        };

        long delayMs = minutes * 60L * 1000L;
        vacancyHandler.postDelayed(vacancyTimeoutRunnable, delayMs);
    }

    public void cancelVacancyTimer() {
        if (vacancyTimeoutRunnable != null) {
            vacancyHandler.removeCallbacks(vacancyTimeoutRunnable);
            vacancyTimeoutRunnable = null;

            if (timerCallback != null && isLightOn) {

            }
        }
    }


    private DatabaseReference getRoomRef(String roomName) {
        if ("Main Office".equals(roomName)) {
            return FirebaseDatabase.getInstance().getReference("room_occupancy");
        } else {
            return FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("room_occupancy");
        }
    }


    private DatabaseReference getLightRef(String roomName) {
        if ("Main Office".equals(roomName)) {
            return FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            return FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
        }
    }

    public void resetVacancyTimer() {

        if (autoLightsOffActive) {
            setAutoLightsOff(currentRoom, true, currentTimeoutMinutes);
        }
    }
}
