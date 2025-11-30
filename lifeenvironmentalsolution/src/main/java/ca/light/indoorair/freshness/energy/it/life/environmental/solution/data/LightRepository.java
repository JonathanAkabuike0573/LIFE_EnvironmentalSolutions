package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LightRepository {

    private DatabaseReference lightSensorDbRef;
    private ValueEventListener lightValueEventListener;
    private final Random random = new Random();
    private static final int SIMULATION_INTERVAL = 4000;

    private void setupFirebaseReference(String roomName) {
        if ("Main Office".equals(roomName)) {
            lightSensorDbRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            lightSensorDbRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
        }
    }

    public void startListening(String roomName, ValueEventListener listener) {
        stopListening();
        setupFirebaseReference(roomName);
        lightValueEventListener = listener;
        lightSensorDbRef.addValueEventListener(listener);
        startSimulation();
    }

    public void stopListening() {
        if (lightValueEventListener != null && lightSensorDbRef != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
            lightValueEventListener = null;
        }
    }

    public void setBrightness(String brightness) {
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("brightness").setValue(brightness);
        }
    }

    public void setAutoBrightness(boolean enabled) {
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("autoBrightness").setValue(enabled);
        }
    }

    private void startSimulation() {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                int currentLux = 50 + random.nextInt(1950);
                Map<String, Object> sensorData = new HashMap<>();
                sensorData.put("lux", currentLux);
                sensorData.put("timestamp", System.currentTimeMillis());
                lightSensorDbRef.updateChildren(sensorData);

                new android.os.Handler(android.os.Looper.getMainLooper())
                        .postDelayed(this, SIMULATION_INTERVAL);
            }
        });
    }
}
