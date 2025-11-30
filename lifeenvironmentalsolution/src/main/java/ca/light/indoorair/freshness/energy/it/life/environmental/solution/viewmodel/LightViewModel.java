package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

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
import java.util.Random;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class LightViewModel extends AndroidViewModel {

    private String currentRoom;
    private DatabaseReference lightSensorDbRef;
    private ValueEventListener lightValueEventListener;

    // LiveData
    private final MutableLiveData<Integer> _lux = new MutableLiveData<>(0);
    public final LiveData<Integer> lux = _lux;

    private final MutableLiveData<String> _lightLevelText = new MutableLiveData<>("Dim");
    public final LiveData<String> lightLevelText = _lightLevelText;

    private final MutableLiveData<String> _lastUpdatedTime = new MutableLiveData<>("--:--");
    public final LiveData<String> lastUpdatedTime = _lastUpdatedTime;

    private final MutableLiveData<Integer> _statusColor = new MutableLiveData<>(R.drawable.circle_indicator_yellow);
    public final LiveData<Integer> statusColor = _statusColor;

    private final MutableLiveData<String> _brightness = new MutableLiveData<>("Neutral");
    public final LiveData<String> brightness = _brightness;

    private final MutableLiveData<Boolean> _autoBrightness = new MutableLiveData<>(true);
    public final LiveData<Boolean> autoBrightness = _autoBrightness;

    private final SharedPreferences sharedPreferences;
    private final Random random = new Random();
    private static final int SIMULATION_INTERVAL = 4000;

    private static final int LUX_DIM_THRESHOLD = 200;
    private static final int LUX_NORMAL_THRESHOLD = 1000;

    public LightViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        this.currentRoom = roomName;
        setupFirebaseReference();
        startListening();
        loadSettings();
    }

    private void setupFirebaseReference() {
        if ("Main Office".equals(currentRoom)) {
            lightSensorDbRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        } else {
            lightSensorDbRef = FirebaseDatabase.getInstance().getReference("rooms").child(currentRoom).child("light");
        }
    }

    private void startListening() {
        if (lightValueEventListener != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
        }

        lightValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Integer luxValue = dataSnapshot.child("lux").getValue(Integer.class);
                    String brightnessValue = dataSnapshot.child("brightness").getValue(String.class);
                    Boolean autoBrightnessValue = dataSnapshot.child("autoBrightness").getValue(Boolean.class);

                    if (luxValue != null) {
                        _lux.setValue(luxValue);
                        updateLightLevelUI(luxValue);
                    }

                    if (brightnessValue != null) {
                        _brightness.setValue(brightnessValue);
                    }

                    if (autoBrightnessValue != null) {
                        _autoBrightness.setValue(autoBrightnessValue);
                        sharedPreferences.edit().putBoolean("auto_brightness_enabled", autoBrightnessValue).apply();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        };

        lightSensorDbRef.addValueEventListener(lightValueEventListener);
        startSimulation();
    }

    public void setBrightness(String brightness) {
        lightSensorDbRef.child("brightness").setValue(brightness);
    }

    public void setAutoBrightness(boolean enabled) {
        lightSensorDbRef.child("autoBrightness").setValue(enabled);
        sharedPreferences.edit().putBoolean("auto_brightness_enabled", enabled).apply();
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

                android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                handler.postDelayed(this, SIMULATION_INTERVAL);
            }
        });
    }

    private void updateLightLevelUI(int lux) {
        String levelText;
        int colorRes;

        if (lux < LUX_DIM_THRESHOLD) {
            levelText = "Dim";
            colorRes = R.drawable.circle_indicator_yellow;
        } else if (lux < LUX_NORMAL_THRESHOLD) {
            levelText = "Normal";
            colorRes = R.drawable.circle_indicator_green;
        } else {
            levelText = "Bright";
            colorRes = R.drawable.circle_indicator_red;
        }

        _lightLevelText.setValue(levelText);
        _statusColor.setValue(colorRes);
        _lastUpdatedTime.setValue(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));
    }

    private void loadSettings() {
        boolean autoEnabled = sharedPreferences.getBoolean("auto_brightness_enabled", true);
        _autoBrightness.setValue(autoEnabled);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (lightValueEventListener != null && lightSensorDbRef != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
        }
    }
}
