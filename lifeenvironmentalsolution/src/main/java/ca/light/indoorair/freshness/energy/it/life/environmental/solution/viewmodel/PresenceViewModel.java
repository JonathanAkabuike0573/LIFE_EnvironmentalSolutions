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
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.PresenceRepository;

public class PresenceViewModel extends AndroidViewModel {

    // Room state
    private String currentRoom;

    // Repository
    private final PresenceRepository presenceRepository;


    private final MutableLiveData<String> _presenceStatus = new MutableLiveData<>("vacant");
    public final LiveData<String> presenceStatus = _presenceStatus;

    private final MutableLiveData<String> _lastUpdatedTime = new MutableLiveData<>("--:--");
    public final LiveData<String> lastUpdatedTime = _lastUpdatedTime;

    private final MutableLiveData<Integer> _sessionDuration = new MutableLiveData<>(0);
    public final LiveData<Integer> sessionDuration = _sessionDuration;

    private final MutableLiveData<Integer> _totalDetections = new MutableLiveData<>(0);
    public final LiveData<Integer> totalDetections = _totalDetections;

    private final MutableLiveData<Boolean> _presenceDetectionEnabled = new MutableLiveData<>(true);
    public final LiveData<Boolean> presenceDetectionEnabled = _presenceDetectionEnabled;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    // Firebase listener
    private ValueEventListener presenceListener;

    // SharedPreferences
    private final SharedPreferences sharedPreferences;

    public PresenceViewModel(@NonNull Application application) {
        super(application);
        presenceRepository = new PresenceRepository();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        this.currentRoom = roomName;
        startListeningForPresence(roomName);
        loadSettings();
    }

    private void startListeningForPresence(String roomName) {

        if (presenceListener != null && currentRoom != null) {
            presenceRepository.stopListening(presenceListener, currentRoom);
        }

        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    if (dataSnapshot.exists()) {
                        DataSnapshot latestReading = getLatestReading(dataSnapshot);

                        if (latestReading != null) {
                            updateFromDataSnapshot(latestReading);
                        } else {
                            setDefaultValues();
                        }
                    } else {
                        setDefaultValues();
                    }
                } catch (Exception e) {
                    _error.setValue("Data parsing error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                _error.setValue("Firebase error: " + databaseError.getMessage());
            }
        };

        presenceRepository.startListening(presenceListener, roomName);
    }

    private DataSnapshot getLatestReading(DataSnapshot dataSnapshot) {
        DataSnapshot latest = null;
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            latest = snapshot;
        }
        return latest;
    }

    private void updateFromDataSnapshot(DataSnapshot snapshot) {

        String roomStatus = safeGetString(snapshot.child("room_status"));
        Long sessionDurationSeconds = safeGetLong(snapshot.child("session_duration_seconds"));
        Object timestampObj = snapshot.child("timestamp").getValue();
        Boolean isManual = snapshot.child("manual_override").getValue(Boolean.class);
        String derivativeLevel = safeGetString(snapshot.child("derivative_level"));


        if (Boolean.TRUE.equals(isManual)) {
            String manualTime = extractTimeFromTimestampSafe(timestampObj);
            _lastUpdatedTime.setValue("Manual Override (" + manualTime + ")");
        } else {
            String formattedTime = formatTimestampSafe(timestampObj);
            if (formattedTime != null) {
                _lastUpdatedTime.setValue(formattedTime);
            }
        }


        if (roomStatus != null) {
            _presenceStatus.setValue(roomStatus);
        }

        if (sessionDurationSeconds != null) {
            _sessionDuration.setValue(sessionDurationSeconds.intValue());
        }

        if (derivativeLevel != null) {
            try {
                _totalDetections.setValue(Integer.parseInt(derivativeLevel));
            } catch (NumberFormatException e) {
                _totalDetections.setValue(0);
            }
        }
    }

    private void setDefaultValues() {
        _presenceStatus.setValue("vacant");
        _sessionDuration.setValue(0);
        _lastUpdatedTime.setValue("--:--");
        _totalDetections.setValue(0);
    }

    public void manualOverride(String status) {

        setPresenceDetectionEnabled(false);

        String manualTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        presenceRepository.manualOverride(status).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _presenceStatus.setValue(status.toLowerCase());
                _sessionDuration.setValue(0);
                _lastUpdatedTime.setValue("Manual Override (" + manualTime + ")");
            } else {
                _error.setValue("Failed to update presence status");
            }
        });
    }


    private String safeGetString(DataSnapshot child) {
        try {
            return child.getValue(String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Long safeGetLong(DataSnapshot child) {
        try {
            return child.getValue(Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatTimestampSafe(Object timestampObj) {
        if (timestampObj == null) return null;

        try {
            if (timestampObj instanceof String) {
                return parseStringTimestamp((String) timestampObj);
            } else if (timestampObj instanceof Long || timestampObj instanceof Number) {
                long timestampMs = ((Number) timestampObj).longValue();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestampMs));
            }
        } catch (Exception e) {

        }
        return timestampObj.toString();
    }

    private String extractTimeFromTimestampSafe(Object timestampObj) {
        if (timestampObj == null) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        }
        try {
            if (timestampObj instanceof String) {
                return parseStringTimestamp((String) timestampObj);
            } else if (timestampObj instanceof Long || timestampObj instanceof Number) {
                long timestampMs = ((Number) timestampObj).longValue();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return sdf.format(new Date(timestampMs));
            }
        } catch (Exception e) {

        }
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private String parseStringTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) return "--:--";

        try {

            long timestampMs = Long.parseLong(rawTimestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(timestampMs));
        } catch (NumberFormatException e1) {

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(rawTimestamp);
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            } catch (Exception e2) {
                return rawTimestamp;
            }
        }
    }

    public void setPresenceDetectionEnabled(boolean enabled) {
        _presenceDetectionEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("presence_detection_enabled", enabled).apply();
    }

    public void setAlertsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("presence_alerts_enabled", enabled).apply();
    }

    public void setAlertStartTime(String time) {
        sharedPreferences.edit().putString("alert_start_time", time).apply();
    }

    public void setAlertEndTime(String time) {
        sharedPreferences.edit().putString("alert_end_time", time).apply();
    }

    private void loadSettings() {
        boolean detectionEnabled = sharedPreferences.getBoolean("presence_detection_enabled", true);
        _presenceDetectionEnabled.setValue(detectionEnabled);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (presenceListener != null && currentRoom != null) {
            presenceRepository.stopListening(presenceListener, currentRoom);
        }
    }
}
