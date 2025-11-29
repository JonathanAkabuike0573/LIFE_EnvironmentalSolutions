package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class PresenceFragment extends Fragment {

    // UI Elements
    private TextView presenceStatusText, lastUpdatedTimeText, sessionDurationText, totalDetectionsText, labelAutoOffTime;
    private ImageView presenceIcon;
    private View statusIndicator;
    private SwitchMaterial presenceDetectionSwitch;
    private Button markOccupiedButton, markEmptyButton;
    private CheckBox autoLightsOffCheckbox, alertAfterHoursCheckbox;
    private Spinner autoOffTimeSpinner;
    private LinearLayout timeRangePickerContainer;
    private TextInputEditText startTimeInput, endTimeInput;

    // Firebase
    private DatabaseReference presenceRef;
    private ValueEventListener presenceListener;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;


    public PresenceFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        initializeViews(view);
        initializeFirebase();
        setupListeners();
        setupSpinner();
    }

    private void initializeViews(View view) {
        presenceStatusText = view.findViewById(R.id.presence_status_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        presenceIcon = view.findViewById(R.id.presence_icon);
        statusIndicator = view.findViewById(R.id.status_indicator);
        presenceDetectionSwitch = view.findViewById(R.id.switch_presence_detection);
        sessionDurationText = view.findViewById(R.id.session_duration_text);
        totalDetectionsText = view.findViewById(R.id.total_detections_text);
        markOccupiedButton = view.findViewById(R.id.button_mark_occupied);
        markEmptyButton = view.findViewById(R.id.button_mark_empty);
        autoLightsOffCheckbox = view.findViewById(R.id.checkbox_auto_lights_off);
        labelAutoOffTime = view.findViewById(R.id.label_auto_off_time);
        autoOffTimeSpinner = view.findViewById(R.id.spinner_auto_off_time);
        alertAfterHoursCheckbox = view.findViewById(R.id.checkbox_alert_after_hours);
        timeRangePickerContainer = view.findViewById(R.id.time_range_picker_container);
        startTimeInput = view.findViewById(R.id.start_time_input);
        endTimeInput = view.findViewById(R.id.end_time_input);
    }

    private void initializeFirebase() {
        presenceRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && presenceDetectionSwitch.isChecked()) {
                    DataSnapshot lastReading = dataSnapshot.getChildren().iterator().next();
                    String roomStatus = lastReading.child("room_status").getValue(String.class);
                    String timestamp = lastReading.child("timestamp").getValue(String.class);
                    Long sessionDuration = lastReading.child("session_duration_seconds").getValue(Long.class);
                    Long totalDetections = lastReading.child("total_detections_today").getValue(Long.class);

                    updatePresenceUI(roomStatus, timestamp, sessionDuration, totalDetections);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Firebase Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
                resetUI();
            }
        };
    }

    private void setupListeners() {
        presenceDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                sharedPreferences.edit().putBoolean("presence_detection_enabled", isChecked).apply();

                if (isChecked) {
                    startListeningForPresence();
                    Toast.makeText(getContext(), "Presence detection enabled", Toast.LENGTH_SHORT).show();
                } else {
                    stopListeningForPresence();
                    Toast.makeText(getContext(), "Presence detection disabled", Toast.LENGTH_SHORT).show();
                }
            }
        });

        autoLightsOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            labelAutoOffTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            autoOffTimeSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        alertAfterHoursCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timeRangePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        startTimeInput.setOnClickListener(v -> showTimePicker(true));
        endTimeInput.setOnClickListener(v -> showTimePicker(false));

        markOccupiedButton.setOnClickListener(v -> manualOverride("Occupied"));
        markEmptyButton.setOnClickListener(v -> manualOverride("Empty"));

        // Listener for changes from other sources (like the dashboard)
        preferenceChangeListener = (prefs, key) -> {
            if (key.equals("presence_detection_enabled")) {
                syncSwitchState();
            }
        };
    }

    private void showTimePicker(boolean isStartTime) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            int hour = picker.getHour();
            int minute = picker.getMinute();
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            if (isStartTime) {
                startTimeInput.setText(formattedTime);
            } else {
                endTimeInput.setText(formattedTime);
            }
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.auto_off_time_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoOffTimeSpinner.setAdapter(adapter);
    }

    private void manualOverride(String status) {
        presenceDetectionSwitch.setChecked(false);
        sharedPreferences.edit().putBoolean("presence_detection_enabled", false).apply();
        updatePresenceUI(status, "Manual Override", 0L, 0L);
        Toast.makeText(getContext(), "Manual override set to " + status, Toast.LENGTH_SHORT).show();
    }

    private void startListeningForPresence() {
        if (presenceRef != null && presenceListener != null) {
            presenceRef.limitToLast(1).addValueEventListener(presenceListener);
        }
    }

    private void stopListeningForPresence() {
        if (presenceRef != null && presenceListener != null) {
            presenceRef.removeEventListener(presenceListener);
        }
        resetUI();
    }

    private void syncSwitchState() {
        boolean isEnabled = sharedPreferences.getBoolean("presence_detection_enabled", true);
        presenceDetectionSwitch.setChecked(isEnabled);
        if (isEnabled) {
            startListeningForPresence();
        } else {
            stopListeningForPresence();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncSwitchState();
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        if (presenceRef != null && presenceListener != null) {
            presenceRef.removeEventListener(presenceListener);
        }
    }

    private void resetUI() {
        presenceStatusText.setText(R.string.not_monitoring);
        lastUpdatedTimeText.setText("--:--");
        presenceIcon.setImageResource(android.R.color.transparent);
        statusIndicator.setBackgroundResource(R.drawable.circle_indicator_gray);
        sessionDurationText.setText("--");
        totalDetectionsText.setText("--");
    }

    private void updatePresenceUI(String status, String timestamp, Long sessionDuration, Long totalDetections) {
        boolean occupied = "occupied".equalsIgnoreCase(status);

        if (occupied) {
            presenceStatusText.setText(R.string.occupied);
            presenceIcon.setImageResource(R.drawable.ic_room_occupied);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            presenceStatusText.setText(R.string.empty);
            presenceIcon.setImageResource(R.drawable.ic_room_empty);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        if (timestamp != null) {
            if (timestamp.equals("Manual Override")) {
                lastUpdatedTimeText.setText(timestamp);
            } else {
                try {
                    SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault());
                    Date date = isoFormat.parse(timestamp);
                    SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                    lastUpdatedTimeText.setText(displayFormat.format(date));
                } catch (ParseException e) {
                    try {
                        SimpleDateFormat isoFormatWithoutMicros = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date date = isoFormatWithoutMicros.parse(timestamp);
                        SimpleDateFormat displayFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                        lastUpdatedTimeText.setText(displayFormat.format(date));
                    } catch (ParseException e2) {
                        if (timestamp.contains("T") && timestamp.contains(".")) {
                            lastUpdatedTimeText.setText(timestamp.substring(timestamp.indexOf('T') + 1, timestamp.indexOf('.')));
                        } else {
                            lastUpdatedTimeText.setText(timestamp);
                        }
                    }
                }
            }
        } else {
            lastUpdatedTimeText.setText(R.string.n_a);
        }

        if (sessionDuration != null) {
            sessionDurationText.setText(String.format(Locale.getDefault(), "%d seconds", sessionDuration));
        } else {
            sessionDurationText.setText("--");
        }

        if (totalDetections != null) {
            totalDetectionsText.setText(String.valueOf(totalDetections));
        } else {
            totalDetectionsText.setText("--");
        }
    }
}
