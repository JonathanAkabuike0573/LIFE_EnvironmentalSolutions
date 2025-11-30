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
    private boolean powerOn = true;
    private ValueEventListener controlListener;

    private final Random random = new Random();
    private static final int SIMULATION_INTERVAL = 4000;


    private boolean autoBrightnessEnabled = true;
    private int sliderBrightnessValue = 50;
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


        controlListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    Boolean pValue = snapshot.child("powerOn").getValue(Boolean.class);
                    if (pValue != null) {
                        powerOn = pValue;
                    }


                    Boolean abValue = snapshot.child("autoBrightness").getValue(Boolean.class);
                    if (abValue != null) {
                        autoBrightnessEnabled = abValue;
                    }


                    Integer sliderValue = snapshot.child("sliderBrightness").getValue(Integer.class);
                    if (sliderValue != null) {
                        sliderBrightnessValue = sliderValue;
                    }

                    stopSimulation();
                    startSmartSimulation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        lightSensorDbRef.addListenerForSingleValueEvent(controlListener);


        startSmartSimulation();
    }


    public void stopListening() {
        if (lightValueEventListener != null && lightSensorDbRef != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
            lightValueEventListener = null;
        }
        if (controlListener != null && lightSensorDbRef != null) {
            lightSensorDbRef.removeEventListener(controlListener);
            controlListener = null;
        }
        stopSimulation();
    }


    public void setPowerOn(boolean enabled) {
        powerOn = enabled;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("powerOn").setValue(enabled);
        }
        stopSimulation();
        startSmartSimulation();
    }

    public void setAutoBrightness(boolean enabled) {
        autoBrightnessEnabled = enabled;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("autoBrightness").setValue(enabled);
        }
        stopSimulation();
        startSmartSimulation();
    }

    public void setBrightness(String brightness) {
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("brightness").setValue(brightness);
        }
    }

    public void setSliderBrightness(int sliderValue) {
        sliderBrightnessValue = sliderValue;
        if (lightSensorDbRef != null) {
            lightSensorDbRef.child("sliderBrightness").setValue(sliderValue);
        }
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

                luxValue = 50 + random.nextInt(1950);
            } else {

                int baseLux = (int) ((sliderBrightnessValue / 100.0) * 2000);

                int fluctuation = (int) (baseLux * 0.1 * (random.nextFloat() - 0.5));
                luxValue = Math.max(0, Math.min(2000, baseLux + fluctuation));
            }

            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("lux", luxValue);
            sensorData.put("timestamp", System.currentTimeMillis());

            lightSensorDbRef.updateChildren(sensorData);


            simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
        }
    };
}
