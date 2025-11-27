package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
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
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;

public class EnergyFragment extends Fragment {

    // --- UI Variables ---
    private TextView lightLevelTitle; // The title text (e.g., "Power Consumption")
    private TextView lightLevelText; // Top Right Status Text (e.g., "High")
    private View statusIndicator; // Top Right Status Indicator (Red/Green Dot)
    private ProgressBar lightLevelProgress; // The Circular Progress Bar
    private TextView lightLevelValueText; // The big number in the center (e.g., "1010")
    private TextView lightLevelUnitText; // The unit below the number (e.g., "W")
    private TextView lastUpdatedTime; // The timestamp at the bottom
    private ChipGroup chipGroupSensorType;

    // --- Firebase Variables ---
    private Query sensorQuery;
    private ValueEventListener sensorListener;
    // Data model to store the latest readings
    private SensorData latestSensorData = new SensorData();

    // --- State Variable ---
    // Enum or simple integer to track the currently selected sensor type
    private static final int TYPE_POWER = 0;
    private static final int TYPE_CURRENT = 1;
    private static final int TYPE_VOLTAGE = 2;
    private int currentDisplayType = TYPE_POWER; // Default to Power

    public EnergyFragment() {
        // Required empty public constructor
    }

    // New inner class to hold all sensor readings
    private static class SensorData {
        public double current_ma = 0.0;
        public double power_w = 0.0;
        public double shunt_v = 0.0;
        public double vin_plus_v = 0.0; // Bus Voltage (V_in+)
        public double vin_minus_v = 0.0; // Shunt Voltage (V_in-)
        public Long timestamp = 0L;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_energy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the views
        lightLevelTitle = view.findViewById(R.id.light_level_title);
        lightLevelText = view.findViewById(R.id.light_level_text);
        statusIndicator = view.findViewById(R.id.status_indicator);
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        lightLevelValueText = view.findViewById(R.id.light_level_value_text);
        lightLevelUnitText = view.findViewById(R.id.light_level_unit_text);
        lastUpdatedTime = view.findViewById(R.id.last_updated_time);

        // Find the new ChipGroup
        chipGroupSensorType = view.findViewById(R.id.chip_group_sensor_type);

        // --- Initialize UI and Listeners ---
        updateUI(currentDisplayType); // Set initial UI (Power)

        chipGroupSensorType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_power)) {
                currentDisplayType = TYPE_POWER;
            } else if (checkedIds.contains(R.id.chip_current)) {
                currentDisplayType = TYPE_CURRENT;
            } else if (checkedIds.contains(R.id.chip_voltage)) {
                currentDisplayType = TYPE_VOLTAGE;
            }
            // Update the UI immediately with the new selection and the latest data
            updateUI(currentDisplayType);
        });

        // Set up the Query to fetch the LAST 1 entry under "ina219_readings"
        sensorQuery = FirebaseDatabase.getInstance()
                .getReference("ina219_readings")
                .limitToLast(1);

        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        // --- Store ALL values from the database into the data model ---

                        // Note: The INA219 readings are:
                        // current_ma (Current in mA)
                        // power_w (Power in W)
                        // vin_plus_v (Bus Voltage V_in+)

                        // Use getValue(Double.class) for all sensor readings
                        Double currentMaValue = childSnapshot.child("current_ma").getValue(Double.class);
                        Double powerWValue = childSnapshot.child("power_w").getValue(Double.class);
                        Double vinPlusVValue = childSnapshot.child("vin_plus_v").getValue(Double.class);
                        Long timestampValue = childSnapshot.child("timestamp").getValue(Long.class);

                        // Update the latestSensorData object
                        latestSensorData.current_ma = (currentMaValue != null) ? currentMaValue : 0.0;
                        latestSensorData.power_w = (powerWValue != null) ? powerWValue : 0.0;
                        latestSensorData.vin_plus_v = (vinPlusVValue != null) ? vinPlusVValue : 0.0;
                        latestSensorData.timestamp = (timestampValue != null) ? timestampValue : 0L;

                        // Now update the UI with the latest data and the current selection
                        updateUI(currentDisplayType);

                        break; // Stop after the first (most recent) child
                    }
                } else {
                    // Handle no data case
                    lightLevelValueText.setText("--");
                    lightLevelProgress.setProgress(0);
                    lightLevelText.setText("No Data");
                    lastUpdatedTime.setText("--:--");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    // New method to update the UI based on the selected type and stored data
    private void updateUI(int displayType) {
        double displayValue;
        String title;
        String unit;
        int maxProgress;

        switch (displayType) {
            case TYPE_CURRENT:
                // Convert mA to Amperes (A) for display
                displayValue = latestSensorData.current_ma / 1000.0;
                title = "Current";
                unit = "A"; // Amperes
                // Set a reasonable max for the progress bar (e.g., 2 Amperes)
                maxProgress = 2;
                break;
            case TYPE_VOLTAGE:
                displayValue = latestSensorData.vin_plus_v; // Bus Voltage
                title = "Bus Voltage";
                unit = "V"; // Volts
                // Set a reasonable max for the progress bar (e.g., 6 Volts)
                maxProgress = 6;
                break;
            case TYPE_POWER:
            default:
                displayValue = latestSensorData.power_w; // Power in Watts
                title = "Power Consumption";
                unit = "W"; // Watts
                // Use the max set in XML for Power (2000)
                maxProgress = 2000;
                break;
        }

        // --- Update Title and Unit ---
        lightLevelTitle.setText(title);
        lightLevelUnitText.setText(unit);

        // Set the max of the progress bar based on the selected type
        lightLevelProgress.setMax(maxProgress);

        // --- Update Value ---
        String formattedValue = String.format(Locale.getDefault(), "%.2f", displayValue);
        lightLevelValueText.setText(formattedValue);

        // --- Update Progress Bar ---
        // Use an integer representation for the progress (may require scaling for current/voltage)
        int progressValue;
        if (displayType == TYPE_CURRENT) {
            // For a Max of 2A, scale the progress (e.g., 0.5A -> 500)
            progressValue = (int) Math.round(displayValue * 1000);
        } else if (displayType == TYPE_VOLTAGE) {
            // For a Max of 6V, scale the progress (e.g., 3V -> 500)
            progressValue = (int) Math.round(displayValue * 100);
        } else {
            progressValue = (int) Math.round(displayValue);
        }

        // Ensure progress doesn't exceed the set max
        lightLevelProgress.setProgress(Math.min(progressValue, maxProgress));

        // --- Update Status Text and Indicator (Logic based on Power) ---
        // You may want to create separate status logic for Voltage/Current later, but for now,
        // let's keep the existing logic based on Power for a quick fix.
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

        // If the displayed value is NOT power, clarify the status text (optional)
        if (displayType != TYPE_POWER) {
            status += " (Power Idle)"; // or remove this if only displaying Power's status
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


    @Override
    public void onResume() {
        super.onResume();
        if (sensorQuery != null && sensorListener != null) {
            sensorQuery.addValueEventListener(sensorListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorQuery != null && sensorListener != null) {
            sensorQuery.removeEventListener(sensorListener);
        }
    }
}