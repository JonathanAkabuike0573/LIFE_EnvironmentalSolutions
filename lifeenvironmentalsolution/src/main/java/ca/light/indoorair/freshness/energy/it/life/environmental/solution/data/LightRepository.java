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

    // Simulation state
    private boolean autoBrightnessEnabled = true;
    private int sliderBrightnessValue = 50; // 0-100
    private android.os.Handler simulationHandler;

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
        startSmartSimulation();
    }

    public void stopListening() {
        if (lightValueEventListener != null && lightSensorDbRef != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
            lightValueEventListener = null;
        }
        stopSimulation();
    }

    public void setAutoBrightness(boolean enabled) {
        autoBrightnessEnabled = enabled;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("autoBrightness").setValue(enabled);
        }
        // Restart simulation with new mode
        stopSimulation();
        startSmartSimulation();
    }

    public void setBrightness(String brightness) {
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("brightness").setValue(brightness);
        }
    }

    public void setSliderBrightness(int sliderValue) { // 0-100
        sliderBrightnessValue = sliderValue;
        // Only affect simulation if auto brightness is OFF
        if (!autoBrightnessEnabled) {
            stopSimulation();
            startSmartSimulation();
        }
    }

    private void startSmartSimulation() {
        simulationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        simulationHandler.post(smartSimulationRunnable);
    }

    private void stopSimulation() {
        if (simulationHandler != null) {
            simulationHandler.removeCallbacks(smartSimulationRunnable);
        }
    }

    private final Runnable smartSimulationRunnable = new Runnable() {
        @Override
        public void run() {
            int luxValue;

            if (autoBrightnessEnabled) {
                // AUTO MODE: Random fluctuation (50-2000 lux)
                luxValue = 50 + random.nextInt(1950);
            } else {
                // SLIDER MODE: Follow slider value (convert 0-100 → 0-2000 lux)
                int baseLux = (int) ((sliderBrightnessValue / 100.0) * 2000);
                // Small fluctuation ±10% around slider value
                int fluctuation = (int) (baseLux * 0.1 * (random.nextFloat() - 0.5));
                luxValue = Math.max(0, Math.min(2000, baseLux + fluctuation));
            }

            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("lux", luxValue);
            sensorData.put("timestamp", System.currentTimeMillis());

            lightSensorDbRef.updateChildren(sensorData);

            // Schedule next update
            simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
        }
    };
}
