package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AirQualityFragment extends Fragment {

    // --- UI Views ---
    private ProgressBar airQualityProgress;
    private TextView airQualityValueText;
    private TextView airQualityLevelText;
    private TextView lastUpdatedTime;
    private View statusIndicator;
    private MaterialButton buttonRefresh;
    private Slider sliderAlertLevel;
    private TextView alertLevelLabel;
    private SwitchMaterial switchAutoVentilation;
    private SwitchMaterial switchPurifierPower;
    private Slider sliderPurifierIntensity;

    // --- Firebase ---
    private DatabaseReference databaseReference;
    private ValueEventListener sensorListener;
    private boolean isFetching = false;

    // --- SharedPreferences for saving settings ---
    private SharedPreferences sharedPreferences;
    public static final String KEY_ALERT_LEVEL = "alert_level";
    public static final String KEY_AUTO_VENT = "auto_vent_enabled";
    public static final String KEY_PURIFIER_POWER = "purifier_power_enabled";
    public static final String KEY_PURIFIER_INTENSITY = "purifier_intensity";

    public AirQualityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_air_quality, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize all UI components by finding their IDs from the layout
        initializeViews(view);

        // Initialize SharedPreferences for storing user settings
        if (getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        // Setup listeners for all interactive controls (sliders, switches, button)
        setupControlListeners();
        // Load any previously saved settings
        loadSettings();

        // Setup Firebase database reference to the correct node
        databaseReference = FirebaseDatabase.getInstance().getReference("sgp30_readings");

        // Start listening for real-time data changes from Firebase
        listenForSensorData();
    }

    private void initializeViews(View view) {
        airQualityProgress = view.findViewById(R.id.air_quality_progress);
        airQualityValueText = view.findViewById(R.id.air_quality_value_text);
        airQualityLevelText = view.findViewById(R.id.air_quality_level_text);
        lastUpdatedTime = view.findViewById(R.id.last_updated_time);
        statusIndicator = view.findViewById(R.id.status_indicator);
        alertLevelLabel = view.findViewById(R.id.alert_level_label);
        buttonRefresh = view.findViewById(R.id.button_refresh);
        sliderAlertLevel = view.findViewById(R.id.slider_alert_level);
        switchAutoVentilation = view.findViewById(R.id.switch_auto_ventilation);
        switchPurifierPower = view.findViewById(R.id.switch_purifier_power);
        sliderPurifierIntensity = view.findViewById(R.id.slider_purifier_intensity);
    }

    private void listenForSensorData() {
        // This is the listener that gets triggered every time data changes in Firebase
        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    DataSnapshot latestReading = null;
                    // The Python script uses .push(), which creates a list. We need to get the last item.
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        latestReading = snapshot;
                    }

                    if (latestReading != null) {
                        // Extract data using the keys from the Python script
                        Long eco2Value = latestReading.child("eCO2").getValue(Long.class);
                        String co2Description = latestReading.child("co2_description").getValue(String.class);
                        String timestamp = latestReading.child("timestamp").getValue(String.class);

                        // Update all the UI elements with the new data
                        updateUI(eco2Value, co2Description, timestamp);
                    }
                } else {
                    // This handles the case where the 'sgp30_readings' node doesn't exist yet
                    updateUI(null, null, null);
                }
                isFetching = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                isFetching = false;
                // Show an error message if Firebase access is denied or fails
                Toast.makeText(getContext(), "Firebase Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                updateUI(null, null, null);
            }
        };

        // This query is very efficient. It tells Firebase to only send us the single most recent reading.
        databaseReference.limitToLast(1).addValueEventListener(sensorListener);
    }

    private void manualRefresh() {
        if (isFetching) {
            Toast.makeText(getContext(), "Already refreshing...", Toast.LENGTH_SHORT).show();
            return;
        }
        isFetching = true;
        Toast.makeText(getContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();

        // This forces a one-time fresh read from the server, bypassing the local cache
        databaseReference.limitToLast(1).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot dataSnapshot = task.getResult();
                if (dataSnapshot.exists()) {
                    // We need to iterate even for one item
                    DataSnapshot latestReading = dataSnapshot.getChildren().iterator().next();
                    Long eco2Value = latestReading.child("eCO2").getValue(Long.class);
                    String co2Description = latestReading.child("co2_description").getValue(String.class);
                    String timestamp = latestReading.child("timestamp").getValue(String.class);
                    updateUI(eco2Value, co2Description, timestamp);
                }
            } else {
                Toast.makeText(getContext(), "Failed to refresh.", Toast.LENGTH_SHORT).show();
            }
            isFetching = false;
        });
    }

    private void updateUI(Long eco2Value, String co2Description, String timestamp) {
        if (getContext() == null) return; // Exit if the fragment is not attached to a context

        if (eco2Value != null) {
            // Update the main UI elements
            airQualityProgress.setProgress(eco2Value.intValue());
            airQualityValueText.setText(String.valueOf(eco2Value));
            airQualityLevelText.setText(co2Description != null ? co2Description : "Unknown");
            lastUpdatedTime.setText(formatTimestamp(timestamp));

            // Save the description to SharedPreferences
            if (co2Description != null) {
                sharedPreferences.edit().putString("air_quality_description", co2Description).apply();
            }

            // Update the color of the status indicator based on the CO2 value
            int color;
            if (eco2Value <= 600) {
                color = ContextCompat.getColor(getContext(), R.color.air_quality_excellent);
            } else if (eco2Value <= 1000) {
                color = ContextCompat.getColor(getContext(), R.color.air_quality_good);
            } else if (eco2Value <= 1500) {
                color = ContextCompat.getColor(getContext(), R.color.air_quality_moderate);
            } else if (eco2Value <= 2000) {
                color = ContextCompat.getColor(getContext(), R.color.air_quality_poor);
            } else {
                color = ContextCompat.getColor(getContext(), R.color.air_quality_very_poor);
            }
            ((GradientDrawable) statusIndicator.getBackground()).setColor(color);
        } else {
            // Handle the case where there is no data (e.g., sensor is offline)
            airQualityProgress.setProgress(0);
            airQualityValueText.setText("--");
            airQualityLevelText.setText("Offline");
            lastUpdatedTime.setText("--:--");
            int grayColor = ContextCompat.getColor(getContext(), R.color.air_quality_offline);
            ((GradientDrawable) statusIndicator.getBackground()).setColor(grayColor);
        }
    }

    private void setupControlListeners() {
        buttonRefresh.setOnClickListener(v -> manualRefresh());

        sliderAlertLevel.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int alertValue = (int) value;
                alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", alertValue));
                sharedPreferences.edit().putInt(KEY_ALERT_LEVEL, alertValue).apply();
            }
        });

        switchAutoVentilation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_AUTO_VENT, isChecked).apply();
            Toast.makeText(getContext(), "Auto Ventilation " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        switchPurifierPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(KEY_PURIFIER_POWER, isChecked).apply();
            sliderPurifierIntensity.setEnabled(isChecked);
            Toast.makeText(getContext(), "Air Purifier " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        sliderPurifierIntensity.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int intensity = (int) value;
                sharedPreferences.edit().putInt(KEY_PURIFIER_INTENSITY, intensity).apply();
                Toast.makeText(getContext(), "Purifier intensity set to " + intensity, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        // Load and apply the saved value for the alert slider
        int savedAlertLevel = sharedPreferences.getInt(KEY_ALERT_LEVEL, 2000);
        sliderAlertLevel.setValue(savedAlertLevel);
        alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", savedAlertLevel));

        // Load and apply the saved state for the auto-ventilation switch
        boolean autoVentEnabled = sharedPreferences.getBoolean(KEY_AUTO_VENT, false);
        switchAutoVentilation.setChecked(autoVentEnabled);

        // Load and apply the saved state for the purifier power switch
        boolean purifierPowerEnabled = sharedPreferences.getBoolean(KEY_PURIFIER_POWER, false);
        switchPurifierPower.setChecked(purifierPowerEnabled);
        sliderPurifierIntensity.setEnabled(purifierPowerEnabled);

        // Load and apply the saved value for the purifier intensity slider
        int purifierIntensity = sharedPreferences.getInt(KEY_PURIFIER_INTENSITY, 1);
        sliderPurifierIntensity.setValue(purifierIntensity);
    }

    private String formatTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isEmpty()) return "--:--";
        try {
            // Input format from Python script: "2024-05-21 10:30:00"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inputFormat.parse(rawTimestamp);
            // Output format for display: "10:30:00"
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return rawTimestamp; // Fallback to raw string if parsing fails
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // This is crucial to prevent memory leaks and stop listening when the fragment is not visible.
        if (databaseReference != null && sensorListener != null) {
            databaseReference.removeEventListener(sensorListener);
        }
    }
}
