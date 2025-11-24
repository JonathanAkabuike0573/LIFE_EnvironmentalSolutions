package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
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

public class LightFragment extends Fragment {

    // UI Elements
    private TextView lightLevelValueText, lightLevelText, lastUpdatedTimeText;
    private ProgressBar lightLevelProgress;
    private View statusIndicator;
    private ChipGroup lightBrightnessChipGroup;
    private SwitchMaterial autoBrightnessSwitch;
    private Slider brightnessSlider;

    // Firebase
    private DatabaseReference lightSensorDbRef;
    private ValueEventListener lightValueEventListener;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private static final String AUTO_BRIGHTNESS_KEY = "auto_brightness_enabled";

    // Simulation logic
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private static final int SIMULATION_INTERVAL = 4000; // 4 seconds

    // Light level thresholds (in LUX)
    private static final int LUX_DIM_THRESHOLD = 200;
    private static final int LUX_NORMAL_THRESHOLD = 1000;

    public LightFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_light, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        initializeViews(view);
        // Initialize Firebase Database reference
        lightSensorDbRef = FirebaseDatabase.getInstance().getReference("sensorData").child("light");
        setupListeners();

    }

    private void initializeViews(View view) {
        lightLevelValueText = view.findViewById(R.id.light_level_value_text);
        lightLevelText = view.findViewById(R.id.light_level_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        statusIndicator = view.findViewById(R.id.status_indicator);
        lightBrightnessChipGroup = view.findViewById(R.id.chip_group_light_brightness);
        autoBrightnessSwitch = view.findViewById(R.id.switch_light_control);
        brightnessSlider = view.findViewById(R.id.slider_brightness);
    }

    private void setupListeners() {
        lightBrightnessChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip != null && selectedChip.isPressed()) {
                String selectedBrightness = selectedChip.getText().toString();
                lightSensorDbRef.child("brightness").setValue(selectedBrightness);
                Toast.makeText(getContext(), selectedBrightness + " selected", Toast.LENGTH_SHORT).show();
            }
        });

        autoBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                sharedPreferences.edit().putBoolean(AUTO_BRIGHTNESS_KEY, isChecked).apply();
                brightnessSlider.setEnabled(!isChecked);
                String message = isChecked ? "Auto brightness enabled" : "Auto brightness disabled";
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                lightSensorDbRef.child("autoBrightness").setValue(isChecked);
            }
        });

        preferenceChangeListener = (prefs, key) -> {
            if (key.equals(AUTO_BRIGHTNESS_KEY)) {
                syncSwitchState();
            }
        };
    }


    @Override
    public void onResume() {
        super.onResume();
        syncSwitchState();
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        startLightLevelSimulationAndDbRead();
    }

    @Override
    public void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        stopLightLevelSimulationAndDbRead();
    }

    private void startLightLevelSimulationAndDbRead() {
        // Start the simulation to write data
        simulationHandler.post(lightLevelRunnable);

        // Start listening for data changes from Firebase
        if (lightValueEventListener == null) {
            lightValueEventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        if (dataSnapshot.hasChild("lux")) {
                            Integer lux = dataSnapshot.child("lux").getValue(Integer.class);
                            if (lux != null) {
                                updateLightLevelUI(lux);
                            }
                        }
                        if (dataSnapshot.hasChild("brightness")) {
                            String brightness = dataSnapshot.child("brightness").getValue(String.class);
                            if (brightness != null) {
                                updateBrightnessSelection(brightness);
                            }
                        }
                        if (dataSnapshot.hasChild("autoBrightness")) {
                            Boolean autoBrightness = dataSnapshot.child("autoBrightness").getValue(Boolean.class);
                            if (autoBrightness != null) {
                                sharedPreferences.edit().putBoolean(AUTO_BRIGHTNESS_KEY, autoBrightness).apply();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            };
        }
        lightSensorDbRef.addValueEventListener(lightValueEventListener);
    }

    private void stopLightLevelSimulationAndDbRead() {
        // Stop the simulation
        simulationHandler.removeCallbacks(lightLevelRunnable);

        // Stop listening for data changes
        if (lightValueEventListener != null) {
            lightSensorDbRef.removeEventListener(lightValueEventListener);
        }
    }

    private final Runnable lightLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Simulate a new light level reading
            int currentLux = 50 + random.nextInt(1950); // Ranges from 50 to 2000 LUX

            // Create a data map to send to Firebase
            Map<String, Object> sensorData = new HashMap<>();
            sensorData.put("lux", currentLux);
            sensorData.put("timestamp", System.currentTimeMillis());

            // Write to Firebase
            lightSensorDbRef.updateChildren(sensorData);

            // Schedule the next update
            simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
        }
    };

    private void updateLightLevelUI(int lux) {
        lightLevelValueText.setText(String.valueOf(lux));
        lightLevelProgress.setProgress(lux);

        // Update the qualitative assessment and status indicator
        if (lux < LUX_DIM_THRESHOLD) {
            lightLevelText.setText(R.string.dim);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_yellow);
        } else if (lux < LUX_NORMAL_THRESHOLD) {
            lightLevelText.setText(R.string.normal);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            lightLevelText.setText("Bright");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        // Update the timestamp
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastUpdatedTimeText.setText(currentTime);
    }

    private void updateBrightnessSelection(String brightness) {
        if (brightness.equals("Warm")) {
            lightBrightnessChipGroup.check(R.id.chip_warm);
        } else if (brightness.equals("Neutral")) {
            lightBrightnessChipGroup.check(R.id.chip_neutral);
        } else if (brightness.equals("Cool")) {
            lightBrightnessChipGroup.check(R.id.chip_cool);
        }
    }

    private void syncSwitchState() {
        boolean isEnabled = sharedPreferences.getBoolean(AUTO_BRIGHTNESS_KEY, true);
        autoBrightnessSwitch.setChecked(isEnabled);
        brightnessSlider.setEnabled(!isEnabled);
    }
}
