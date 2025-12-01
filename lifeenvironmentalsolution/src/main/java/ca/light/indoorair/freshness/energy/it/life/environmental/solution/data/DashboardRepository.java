package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardRepository {

    // LiveData for ViewModel
    private final MutableLiveData<Double> temperatureC = new MutableLiveData<>();
    public LiveData<Double> getTemperatureC() { return temperatureC; }

    private final MutableLiveData<Double> temperatureF = new MutableLiveData<>();
    public LiveData<Double> getTemperatureF() { return temperatureF; }

    private final MutableLiveData<String> comfortLevel = new MutableLiveData<>("No data");
    public LiveData<String> getComfortLevel() { return comfortLevel; }

    private final MutableLiveData<String> airQuality = new MutableLiveData<>("Good");
    public LiveData<String> getAirQuality() { return airQuality; }

    private final MutableLiveData<Boolean> smartLightPower = new MutableLiveData<>(false);
    public LiveData<Boolean> getSmartLightPower() { return smartLightPower; }

    // Firebase listeners
    private DatabaseReference roomRef, airQualityRef, lightRef;
    private ValueEventListener roomListener, airQualityListener, lightListener;

    public void init(String roomName) {
        cleanup();
        setupReferences(roomName);
        attachListeners();
    }

    private void setupReferences(String roomName) {
        if ("Main Office".equals(roomName)) {
            roomRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
            airQualityRef = FirebaseDatabase.getInstance().getReference("sgp30_readings");
            lightRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("room_occupancy");
            airQualityRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("air_quality_readings");
            lightRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("light");
        }
    }

    private void attachListeners() {

        roomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot lastReading = null;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        lastReading = child;
                    }
                    if (lastReading != null) {
                        temperatureC.setValue(lastReading.child("current_temperature_c").getValue(Double.class));
                        temperatureF.setValue(lastReading.child("current_temperature_f").getValue(Double.class));
                        comfortLevel.setValue(lastReading.child("comfort_level").getValue(String.class));
                    } else {
                        temperatureC.setValue(null);
                        temperatureF.setValue(null);
                        comfortLevel.setValue("No data");
                    }
                } else {
                    temperatureC.setValue(null);
                    temperatureF.setValue(null);
                    comfortLevel.setValue("No data");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        roomRef.addValueEventListener(roomListener);


        airQualityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSnapshot lastReading = null;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        lastReading = child;
                    }
                    airQuality.setValue(lastReading != null ?
                            lastReading.child("co2_description").getValue(String.class) : "Good");
                } else {
                    airQuality.setValue("Good");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        airQualityRef.limitToLast(1).addValueEventListener(airQualityListener);

        // Smart Light
        lightListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean powerOn = snapshot.child("powerOn").getValue(Boolean.class);
                smartLightPower.setValue(powerOn != null && powerOn);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        lightRef.addValueEventListener(lightListener);
    }

    public void toggleSmartLightPower(boolean newState) {
        if (lightRef != null) {
            lightRef.child("powerOn").setValue(newState);
        }
    }

    private void cleanup() {
        if (roomRef != null && roomListener != null) {
            roomRef.removeEventListener(roomListener);
        }
        if (airQualityRef != null && airQualityListener != null) {
            airQualityRef.removeEventListener(airQualityListener);
        }
        if (lightRef != null && lightListener != null) {
            lightRef.removeEventListener(lightListener);
        }
    }

    public void destroy() {
        cleanup();
    }
}
