package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class EnergyFragment extends Fragment {

    // --- UI Variables ---
    private TextView lightLevelTitle;
    private TextView lightLevelText;
    private View statusIndicator;
    private ProgressBar lightLevelProgress;
    private TextView lightLevelValueText;
    private TextView lightLevelUnitText;
    private TextView lastUpdatedTime;
    private ChipGroup chipGroupSensorType;

    // --- Firebase Variables ---
    private Query sensorQuery;
    private ValueEventListener sensorListener;
    private final SensorData latestSensorData = new SensorData();

    // --- State Variables ---
    private static final int TYPE_POWER = 0;
    private static final int TYPE_CURRENT = 1;
    private static final int TYPE_VOLTAGE = 2;
    private int currentDisplayType = TYPE_POWER; // Default to Power

    public EnergyFragment() {
        // Required empty public constructor
    }

    private static class SensorData {
        public double current_ma = 0.0;
        public double power_w = 0.0;
        public double vin_plus_v = 0.0; // Bus Voltage (V_in+)
        public Long timestamp = 0L;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_energy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- 1. Find all views ---
        initializeViews(view);

        // --- 2. Set up UI Listeners ---
        setupChipGroupListener();

        // --- 3. Set an initial loading state ---
        setInitialLoadingState();

        // --- 4. Set up Firebase Query and Listener ---
        setupFirebaseListener();
    }

    private void initializeViews(View view) {
        lightLevelTitle = view.findViewById(R.id.light_level_title);
        lightLevelText = view.findViewById(R.id.light_level_text);
        statusIndicator = view.findViewById(R.id.status_indicator);
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        lightLevelValueText = view.findViewById(R.id.light_level_value_text);
        lightLevelUnitText = view.findViewById(R.id.light_level_unit_text);
        lastUpdatedTime = view.findViewById(R.id.last_updated_time);
        chipGroupSensorType = view.findViewById(R.id.chip_group_sensor_type);
    }

    private void setupChipGroupListener() {
        chipGroupSensorType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            // Get the first ID from the list of checked IDs.
            // For a single-selection group, this list will have at most one element.
            List<Integer> ids = group.getCheckedChipIds();
            if (ids.isEmpty()) return; // Do nothing if no chip is selected

            int checkedId = ids.get(0);

            if (checkedId == R.id.chip_power) {
                currentDisplayType = TYPE_POWER;
            } else if (checkedId == R.id.chip_current) {
                currentDisplayType = TYPE_CURRENT;
            } else if (checkedId == R.id.chip_voltage) {
                currentDisplayType = TYPE_VOLTAGE;
            }
            // Update the UI immediately with the new selection and the latest stored data
            updateUI(currentDisplayType);
        });
    }

    private void setInitialLoadingState() {
        lightLevelTitle.setText("Power Consumption");
        lightLevelUnitText.setText("W");
        lightLevelValueText.setText("...");
        lightLevelText.setText("Loading...");
        statusIndicator.getBackground().setTint(Color.GRAY);
        lastUpdatedTime.setText("--:--");
        lightLevelProgress.setProgress(0);
    }

    private void setupFirebaseListener() {
        sensorQuery = FirebaseDatabase.getInstance()
                .getReference("ina219_readings")
                .limitToLast(1);

        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        Double currentMaValue = childSnapshot.child("current_ma").getValue(Double.class);
                        Double powerWValue = childSnapshot.child("power_w").getValue(Double.class);
                        Double vinPlusVValue = childSnapshot.child("vin_plus_v").getValue(Double.class);

                        Object timestampObject = childSnapshot.child("timestamp").getValue();
                        Long timestampValue = 0L; // Default to 0

                        if (timestampObject instanceof Long) {
                            // If it's already a number (Long), use it directly.
                            timestampValue = (Long) timestampObject;
                        } else if (timestampObject instanceof String) {
                            // If it's a String, try to parse it into a Long.
                            try {
                                timestampValue = Long.parseLong((String) timestampObject);
                            } catch (NumberFormatException e) {
                                // Could not parse the string, log the error and keep timestamp as 0.
                                System.err.println("Firebase timestamp was a non-numeric string: " + timestampObject);
                            }
                        }


                        latestSensorData.current_ma = (currentMaValue != null) ? currentMaValue : 0.0;
                        latestSensorData.power_w = (powerWValue != null) ? powerWValue : 0.0;
                        latestSensorData.vin_plus_v = (vinPlusVValue != null) ? vinPlusVValue : 0.0;
                        latestSensorData.timestamp = timestampValue; // Use the safely parsed value

                        updateUI(currentDisplayType);

                        break;
                    }
                } else {
                    updateUIForNoData();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                updateUIForNoData();
            }
        };
    }

    private void updateUI(int displayType) {
        if (!isAdded()) {
            // Ensure fragment is still attached to an activity before proceeding
            return;
        }

        double displayValue;
        String title;
        String unit;
        int maxProgress;

        switch (displayType) {
            case TYPE_CURRENT:
                displayValue = latestSensorData.current_ma / 1000.0;
                title = "Current";
                unit = "A";
                maxProgress = 2; // Max 2 Amperes
                break;
            case TYPE_VOLTAGE:
                displayValue = latestSensorData.vin_plus_v;
                title = "Bus Voltage";
                unit = "V";
                maxProgress = 6; // Max 6 Volts
                break;
            case TYPE_POWER:
            default:
                displayValue = latestSensorData.power_w;
                title = "Power Consumption";
                unit = "W";
                maxProgress = 2000; // As defined in XML
                break;
        }

        lightLevelTitle.setText(title);
        lightLevelUnitText.setText(unit);
        lightLevelProgress.setMax(maxProgress);

        String formattedValue = String.format(Locale.getDefault(), "%.2f", displayValue);
        lightLevelValueText.setText(formattedValue);

        int progressValue;
        if (displayType == TYPE_CURRENT) {
            progressValue = (int) Math.round(displayValue * 1000); // Scale for progress bar
        } else if (displayType == TYPE_VOLTAGE) {
            progressValue = (int) Math.round(displayValue * 100); // Scale for progress bar
        } else {
            progressValue = (int) Math.round(displayValue);
        }
        lightLevelProgress.setProgress(Math.min(progressValue, maxProgress));

        // --- Update Status Text and Indicator (based on power) ---
        int powerInt = (int) Math.round(latestSensorData.power_w);
        String status;
        int indicatorColor;
        int green = ContextCompat.getColor(requireContext(), R.color.green_light);
        int yellow = ContextCompat.getColor(requireContext(), R.color.yellow);
        int red = Color.RED;

        if (powerInt < 50) {
            status = "Idle";
            indicatorColor = green;
        } else if (powerInt < 500) {
            status = "Moderate";
            indicatorColor = yellow;
        } else {
            status = "High";
            indicatorColor = red;
        }

        lightLevelText.setText(status);
        statusIndicator.getBackground().setTint(indicatorColor);

        // --- Update Timestamp ---
        if (latestSensorData.timestamp > 0) {
            String timeString = android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date(latestSensorData.timestamp)).toString();
            lastUpdatedTime.setText(timeString);
        } else {
            lastUpdatedTime.setText("--:--");
        }
    }

    private void updateUIForNoData() {
        if (!isAdded()) return;
        lightLevelValueText.setText("--");
        lightLevelProgress.setProgress(0);
        lightLevelText.setText("No Data");
        statusIndicator.getBackground().setTint(Color.GRAY);
        lastUpdatedTime.setText("--:--");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start listening for data when the fragment becomes visible
        if (sensorQuery != null && sensorListener != null) {
            sensorQuery.addValueEventListener(sensorListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop listening when the fragment is no longer visible to save resources
        if (sensorQuery != null && sensorListener != null) {
            sensorQuery.removeEventListener(sensorListener);
        }
    }
}
