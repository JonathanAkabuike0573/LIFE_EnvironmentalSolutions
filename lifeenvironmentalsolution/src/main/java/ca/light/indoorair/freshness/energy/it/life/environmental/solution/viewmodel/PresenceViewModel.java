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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.NotificationRepository;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.PresenceRepository;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.model.Notification;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.util.NotificationHelper;

public class PresenceViewModel extends AndroidViewModel {

    private final PresenceRepository presenceRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationHelper notificationHelper;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<String> _presenceStatus = new MutableLiveData<>();
    public final LiveData<String> presenceStatus = _presenceStatus;

    private final MutableLiveData<String> _lastUpdatedTime = new MutableLiveData<>();
    public final LiveData<String> lastUpdatedTime = _lastUpdatedTime;

    private final MutableLiveData<Long> _sessionDuration = new MutableLiveData<>();
    public final LiveData<Long> sessionDuration = _sessionDuration;

    private final MutableLiveData<Long> _totalDetections = new MutableLiveData<>();
    public final LiveData<Long> totalDetections = _totalDetections;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _presenceDetectionEnabled = new MutableLiveData<>();
    public final LiveData<Boolean> presenceDetectionEnabled = _presenceDetectionEnabled;

    private String alertStartTime;
    private String alertEndTime;
    private boolean isAlertsEnabled = false;

    private ValueEventListener presenceListener;

    public PresenceViewModel(@NonNull Application application) {
        super(application);
        presenceRepository = new PresenceRepository();
        notificationRepository = new NotificationRepository();
        notificationHelper = new NotificationHelper(application);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(application);
    }

    public void init() {
        boolean isEnabled = sharedPreferences.getBoolean("presence_detection_enabled", true);
        isAlertsEnabled = sharedPreferences.getBoolean("alert_after_hours", false);
        alertStartTime = sharedPreferences.getString("alert_start_time", null);
        alertEndTime = sharedPreferences.getString("alert_end_time", null);
        _presenceDetectionEnabled.setValue(isEnabled);
        if (isEnabled) {
            startListeningForPresence();
        }
    }

    public void setPresenceDetectionEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("presence_detection_enabled", enabled).apply();
        _presenceDetectionEnabled.setValue(enabled);
        if (enabled) {
            startListeningForPresence();
        } else {
            stopListeningForPresence();
        }
    }

    public void setAlertsEnabled(boolean enabled) {
        isAlertsEnabled = enabled;
        sharedPreferences.edit().putBoolean("alert_after_hours", enabled).apply();
    }

    public void setAlertStartTime(String time) {
        alertStartTime = time;
        sharedPreferences.edit().putString("alert_start_time", time).apply();
    }

    public void setAlertEndTime(String time) {
        alertEndTime = time;
        sharedPreferences.edit().putString("alert_end_time", time).apply();
    }

    public void manualOverride(String status) {
        setPresenceDetectionEnabled(false);
        presenceRepository.manualOverride(status);
        _presenceStatus.setValue(status);
        _lastUpdatedTime.setValue("Manual Override");
        _sessionDuration.setValue(0L);
        _totalDetections.setValue(0L);
    }

    private void startListeningForPresence() {
        if (presenceListener == null) {
            presenceListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        DataSnapshot lastReading = dataSnapshot.getChildren().iterator().next();
                        String roomStatus = lastReading.child("room_status").getValue(String.class);
                        String timestamp = lastReading.child("timestamp").getValue(String.class);
                        Long sessionDuration = lastReading.child("session_duration_seconds").getValue(Long.class);
                        Long totalDetections = lastReading.child("total_detections_today").getValue(Long.class);

                        _presenceStatus.setValue(roomStatus);
                        _lastUpdatedTime.setValue(formatTimestamp(timestamp));
                        _sessionDuration.setValue(sessionDuration);
                        _totalDetections.setValue(totalDetections);

                        if (isAlertsEnabled && "occupied".equalsIgnoreCase(roomStatus)) {
                            checkTimeAndSendNotification();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    _error.setValue("Firebase Error: " + databaseError.getMessage());
                }
            };
        }
        presenceRepository.startListening(presenceListener);
    }

    private void stopListeningForPresence() {
        if (presenceListener != null) {
            presenceRepository.stopListening(presenceListener);
        }
        // Reset values when not listening
        _presenceStatus.setValue("Not Monitoring");
        _lastUpdatedTime.setValue("--:--");
        _sessionDuration.setValue(null);
        _totalDetections.setValue(null);
    }

    private void checkTimeAndSendNotification() {
        if (alertStartTime == null || alertEndTime == null) {
            return;
        }

        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date startTime = timeFormat.parse(alertStartTime);
            Date endTime = timeFormat.parse(alertEndTime);
            Date now = new Date();

            Calendar startCal = Calendar.getInstance();
            startCal.setTime(startTime);

            Calendar endCal = Calendar.getInstance();
            endCal.setTime(endTime);

            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(now);

            startCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
            startCal.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
            startCal.set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH));

            endCal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR));
            endCal.set(Calendar.MONTH, nowCal.get(Calendar.MONTH));
            endCal.set(Calendar.DAY_OF_MONTH, nowCal.get(Calendar.DAY_OF_MONTH));

            if (nowCal.after(startCal) && nowCal.before(endCal)) {
                String title = "Presence Detected";
                String message = "Presence has been detected in the room.";
                notificationHelper.sendNotification(title, message);
                Notification notification = new Notification(title, message, System.currentTimeMillis());
                notificationRepository.saveNotification(notification);
            }
        } catch (ParseException e) {
            _error.setValue("Error parsing alert times.");
        }
    }

    private String formatTimestamp(String timestamp) {
        if (timestamp == null) {
            return "N/A";
        }
        if (timestamp.equals("Manual Override")) {
            return timestamp;
        }
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
            Date date = isoFormat.parse(timestamp);
            SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return displayFormat.format(date);
        } catch (ParseException e) {
            try {
                SimpleDateFormat isoFormatWithoutMicros = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = isoFormatWithoutMicros.parse(timestamp);
                SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                return displayFormat.format(date);
            } catch (ParseException e2) {
                if (timestamp.contains("T") && timestamp.contains(".")) {
                    return timestamp.substring(timestamp.indexOf('T') + 1, timestamp.indexOf('.'));
                } else {
                    return timestamp;
                }
            }
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListeningForPresence();
    }
}
