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

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.PresenceRepository;

public class PresenceViewModel extends AndroidViewModel {

    private String currentRoom;
    private final PresenceRepository presenceRepository;
    private final SharedPreferences sharedPreferences;


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


    private final MutableLiveData<Boolean> _autoLightsOffEnabled = new MutableLiveData<>(false);
    public final LiveData<Boolean> autoLightsOffEnabled = _autoLightsOffEnabled;

    private final MutableLiveData<Integer> _autoOffTimeoutMinutes = new MutableLiveData<>(5);
    public final LiveData<Integer> autoOffTimeoutMinutes = _autoOffTimeoutMinutes;


    private final MutableLiveData<Boolean> _alertsEnabled = new MutableLiveData<>(false);
    public final LiveData<Boolean> alertsEnabled = _alertsEnabled;

    private final MutableLiveData<String> _alertStartTime = new MutableLiveData<>("18:00");
    public final LiveData<String> alertStartTime = _alertStartTime;

    private final MutableLiveData<String> _alertEndTime = new MutableLiveData<>("08:00");
    public final LiveData<String> alertEndTime = _alertEndTime;

    private final MutableLiveData<String> _timerEventMessage = new MutableLiveData<>();
    public final LiveData<String> timerEventMessage = _timerEventMessage;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private ValueEventListener presenceListener;

    public PresenceViewModel(@NonNull Application application) {
        super(application);

        presenceRepository = new PresenceRepository(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init(String roomName) {
        this.currentRoom = roomName;

        presenceRepository.setTimerCallback(message -> {
            _timerEventMessage.postValue(message);
        });

        loadSettings();

        if (Boolean.TRUE.equals(_presenceDetectionEnabled.getValue())) {
            startListeningForPresence(roomName);

            presenceRepository.setAutoLightsOff(roomName,
                    Boolean.TRUE.equals(_autoLightsOffEnabled.getValue()),
                    _autoOffTimeoutMinutes.getValue());
        } else {
            _presenceStatus.setValue("disabled");
            _lastUpdatedTime.setValue("Monitoring Paused");
        }
    }



    public void setPresenceDetectionEnabled(boolean enabled) {
        _presenceDetectionEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("presence_detection_enabled", enabled).apply();

        if (enabled) {
            startListeningForPresence(currentRoom);
            presenceRepository.setAutoLightsOff(currentRoom,
                    Boolean.TRUE.equals(_autoLightsOffEnabled.getValue()),
                    _autoOffTimeoutMinutes.getValue());
        } else {
            if (presenceListener != null && currentRoom != null) {
                presenceRepository.stopListening(presenceListener, currentRoom);
            }

            presenceRepository.setAutoLightsOff(currentRoom, false, 0);

            _presenceStatus.setValue("disabled");
            _lastUpdatedTime.setValue("Monitoring Paused");
            _sessionDuration.setValue(0);
            _totalDetections.setValue(0);
        }
    }

    public void setAutoLightsOffEnabled(boolean enabled, int timeoutMinutes) {
        _autoLightsOffEnabled.setValue(enabled);
        _autoOffTimeoutMinutes.setValue(timeoutMinutes);
        sharedPreferences.edit()
                .putBoolean("auto_lights_off_enabled", enabled)
                .putInt("auto_off_timeout_minutes", timeoutMinutes)
                .apply();

        if (Boolean.TRUE.equals(_presenceDetectionEnabled.getValue())) {
            presenceRepository.setAutoLightsOff(currentRoom, enabled, timeoutMinutes);
        }
    }

    public void setAlertsEnabled(boolean enabled) {
        _alertsEnabled.setValue(enabled);
        sharedPreferences.edit().putBoolean("presence_alerts_enabled", enabled).apply();
        updateRepoAlertSettings();
    }

    public void setAlertStartTime(String time) {
        _alertStartTime.setValue(time);
        sharedPreferences.edit().putString("alert_start_time", time).apply();
        updateRepoAlertSettings();
    }

    public void setAlertEndTime(String time) {
        _alertEndTime.setValue(time);
        sharedPreferences.edit().putString("alert_end_time", time).apply();
        updateRepoAlertSettings();
    }

    private void updateRepoAlertSettings() {
        presenceRepository.setAlertSettings(
                Boolean.TRUE.equals(_alertsEnabled.getValue()),
                _alertStartTime.getValue(),
                _alertEndTime.getValue()
        );
    }



    private void startListeningForPresence(String roomName) {
        if (presenceListener != null && currentRoom != null) {
            presenceRepository.stopListening(presenceListener, currentRoom);
        }

        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!Boolean.TRUE.equals(_presenceDetectionEnabled.getValue())) return;
                try {
                    if (dataSnapshot.exists()) {
                        DataSnapshot latestReading = getLatestReading(dataSnapshot);
                        if (latestReading != null) updateFromDataSnapshot(latestReading);
                        else setDefaultValues();
                    } else setDefaultValues();
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
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) latest = snapshot;
        return latest;
    }

    private void updateFromDataSnapshot(DataSnapshot snapshot) {

        String roomStatus = safeGetString(snapshot.child("room_status"));
        Long sessionDurationSeconds = safeGetLong(snapshot.child("session_duration_seconds"));

        if (roomStatus != null) _presenceStatus.setValue(roomStatus);
        if (sessionDurationSeconds != null) _sessionDuration.setValue(sessionDurationSeconds.intValue());
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
                _lastUpdatedTime.setValue("Manual Override (" + manualTime + ")");
            }
        });
    }

    private String safeGetString(DataSnapshot child) { try { return child.getValue(String.class); } catch (Exception e) { return null; } }
    private Long safeGetLong(DataSnapshot child) { try { return child.getValue(Long.class); } catch (Exception e) { return null; } }

    private void loadSettings() {
        _presenceDetectionEnabled.setValue(sharedPreferences.getBoolean("presence_detection_enabled", true));
        _autoLightsOffEnabled.setValue(sharedPreferences.getBoolean("auto_lights_off_enabled", false));
        _autoOffTimeoutMinutes.setValue(sharedPreferences.getInt("auto_off_timeout_minutes", 5));


        _alertsEnabled.setValue(sharedPreferences.getBoolean("presence_alerts_enabled", false));
        _alertStartTime.setValue(sharedPreferences.getString("alert_start_time", "18:00"));
        _alertEndTime.setValue(sharedPreferences.getString("alert_end_time", "08:00"));


        updateRepoAlertSettings();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (presenceListener != null && currentRoom != null) {
            presenceRepository.stopListening(presenceListener, currentRoom);
        }
        presenceRepository.setAutoLightsOff(currentRoom, false, 0);
    }
}
