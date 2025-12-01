package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;
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
    private boolean powerOn = true;
    private int sliderBrightnessValue = 50;
    private android.os.Handler simulationHandler;
    private boolean isSimulating = false;


    private DatabaseReference getRefForRoom(String roomName) {
        if ("Main Office".equals(roomName)) {
            return FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            return FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
        }
    }

    private void setupFirebaseReference(String roomName) {
        lightSensorDbRef = getRefForRoom(roomName);
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


    public void setPowerOn(boolean enabled) {
        powerOn = enabled;

        if (lightSensorDbRef != null) {
            performLightUpdate(lightSensorDbRef, enabled);
        }
        stopSimulation();
    }


    public void setPowerOn(String roomName, boolean enabled) {

        DatabaseReference tempRef = getRefForRoom(roomName);
        performLightUpdate(tempRef, enabled);
    }


    private void performLightUpdate(DatabaseReference ref, boolean enabled) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("powerOn", enabled);

        if (!enabled) {

            autoBrightnessEnabled = false;
            updates.put("autoBrightness", false);
            updates.put("lux", 0);
            updates.put("sliderBrightness", 0);
        }

        ref.updateChildren(updates);
    }

    public void setAutoBrightness(boolean enabled) {
        if (!powerOn) return;
        autoBrightnessEnabled = enabled;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("autoBrightness").setValue(enabled);
        }
        restartSimulation();
    }

    public void setBrightness(String brightness) {
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("brightness").setValue(brightness);
        }
    }

    public void setSliderBrightness(int sliderValue) {
        if (!powerOn) return;
        sliderBrightnessValue = sliderValue;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("sliderBrightness").setValue(sliderValue);
        }
        if (!autoBrightnessEnabled) {
            restartSimulation();
        }
    }

    private void restartSimulation() {
        stopSimulation();
        if (powerOn) {
            startSmartSimulation();
        }
    }

    private void startSmartSimulation() {
        if (isSimulating || !powerOn) return;
        isSimulating = true;
        simulationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        simulationHandler.post(smartSimulationRunnable);
    }

    private void stopSimulation() {
        isSimulating = false;
        if (simulationHandler != null) {
            simulationHandler.removeCallbacks(smartSimulationRunnable);
            simulationHandler = null;
        }
    }

    private final Runnable smartSimulationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!powerOn || !isSimulating) {
                if (lightSensorDbRef != null) {
                    lightSensorDbRef.child("lux").setValue(0);
                }
                stopSimulation();
                return;
            }

            int luxValue;
            if (autoBrightnessEnabled) {
                luxValue = 50 + random.nextInt(1950);
            } else {
                int baseLux = (int) ((sliderBrightnessValue / 100.0) * 2000);
                int fluctuation = (int) (baseLux * 0.1 * (random.nextFloat() - 0.5));
                luxValue = Math.max(0, Math.min(2000, baseLux + fluctuation));
            }

            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("lux", luxValue);
            sensorData.put("timestamp", System.currentTimeMillis());

            if (lightSensorDbRef != null) {
                lightSensorDbRef.updateChildren(sensorData);
            }

            if (powerOn && isSimulating) {
                simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
            }
        }
    };
}
