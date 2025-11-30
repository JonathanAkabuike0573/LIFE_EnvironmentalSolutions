package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.AirQualityRepository;

public class AirQualityViewModel extends AndroidViewModel {

    // Keys for SharedPreferences
    public static final String KEY_ALERT_LEVEL = "alert_level";
    public static final String KEY_AUTO_VENT = "auto_vent_enabled";
    public static final String KEY_PURIFIER_POWER = "purifier_power_enabled";
    public static final String KEY_PURIFIER_INTENSITY = "purifier_intensity";

    private final AirQualityRepository airQualityRepository;
    private final SharedPreferences sharedPreferences;

    // LiveData for UI state
    private final MutableLiveData<Integer> _airQualityValue = new MutableLiveData<>();
    public final LiveData<Integer> airQualityValue = _airQualityValue;

    private final MutableLiveData<String> _airQualityLevel = new MutableLiveData<>();
    public final LiveData<String> airQualityLevel = _airQualityLevel;

    private final MutableLiveData<String> _lastUpdatedTime = new MutableLiveData<>();
    public final LiveData<String> lastUpdatedTime = _lastUpdatedTime;

    private final MutableLiveData<Integer> _statusColor = new MutableLiveData<>();
    public final LiveData<Integer> statusColor = _statusColor;

    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRefreshing = _isRefreshing;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private ValueEventListener sensorListener;
    private String currentRoom;

    public AirQualityViewModel(@NonNull Application application) {
        super(application);
        airQualityRepository = new AirQualityRepository();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        this.currentRoom = roomName;
        listenForSensorData(roomName);
    }

    private void listenForSensorData(String roomName) {
        if (sensorListener != null && currentRoom != null) {
            airQualityRepository.removeListener(sensorListener, currentRoom);
        }
        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                processDataSnapshot(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                _error.setValue("Firebase Error: " + databaseError.getMessage());
                updateUI(null, null, null);
            }
        };
        airQualityRepository.listenForSensorData(sensorListener, roomName);
    }

    public void manualRefresh() {
        if (_isRefreshing.getValue() != null && _isRefreshing.getValue()) {
            return;
        }
        _isRefreshing.setValue(true);
        airQualityRepository.manualRefresh(currentRoom).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                processDataSnapshot(task.getResult());
            } else {
                _error.setValue("Failed to refresh.");
            }
            _isRefreshing.setValue(false);
        });
    }

    private void processDataSnapshot(DataSnapshot dataSnapshot) {
        if (dataSnapshot.exists()) {
            DataSnapshot latestReading = null;
            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                latestReading = snapshot;
            }
            if (latestReading != null) {
                Long eco2Value = latestReading.child("eCO2").getValue(Long.class);
                String co2Description = latestReading.child("co2_description").getValue(String.class);
                String timestamp = latestReading.child("timestamp").getValue(String.class);
                updateUI(eco2Value, co2Description, timestamp);
            } else {
                updateUI(null, null, null);
            }
        } else {
            updateUI(null, null, null);
        }
    }

    private void updateUI(Long eco2Value, String co2Description, String timestamp) {
        if (eco2Value != null) {
            _airQualityValue.setValue(eco2Value.intValue());
            _airQualityLevel.setValue(co2Description != null ? co2Description : "Unknown");
            _lastUpdatedTime.setValue(formatTimestamp(timestamp));

            if (co2Description != null) {
                sharedPreferences.edit().putString("air_quality_description", co2Description).apply();
            }

            if (eco2Value <= 600) {
                _statusColor.setValue(R.color.air_quality_excellent);
            } else if (eco2Value <= 1000) {
                _statusColor.setValue(R.color.air_quality_good);
            } else if (eco2Value <= 1500) {
                _statusColor.setValue(R.color.air_quality_moderate);
            } else if (eco2Value <= 2000) {
                _statusColor.setValue(R.color.air_quality_poor);
            } else {
                _statusColor.setValue(R.color.air_quality_very_poor);
            }
        } else {
            _airQualityValue.setValue(0);
            _airQualityLevel.setValue("Offline");
            _lastUpdatedTime.setValue("--:--");
            _statusColor.setValue(R.color.air_quality_offline);
        }
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) return "--:--";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(rawTimestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawTimestamp;
        }
    }

    public void saveIntSetting(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    public void saveBooleanSetting(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public int getIntSetting(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (sensorListener != null && currentRoom != null) {
            airQualityRepository.removeListener(sensorListener, currentRoom);
        }
    }
}