package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class LightFragment extends Fragment {

    // UI Elements
    private TextView lightLevelValueText, lightLevelText, lastUpdatedTimeText;
    private ProgressBar lightLevelProgress;
    private View statusIndicator;

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
        initializeViews(view);
    }

    private void initializeViews(View view) {
        lightLevelValueText = view.findViewById(R.id.light_level_value_text);
        lightLevelText = view.findViewById(R.id.light_level_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        statusIndicator = view.findViewById(R.id.status_indicator);
    }

    @Override
    public void onResume() {
        super.onResume();
        startLightLevelSimulation();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLightLevelSimulation();
    }

    private void startLightLevelSimulation() {
        simulationHandler.post(lightLevelRunnable);
    }

    private void stopLightLevelSimulation() {
        simulationHandler.removeCallbacks(lightLevelRunnable);
    }

    private final Runnable lightLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Simulate a new light level reading
            int currentLux = 50 + random.nextInt(1950); // Ranges from 50 to 2000 LUX
            updateLightLevelUI(currentLux);

            // Schedule the next update
            simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
        }
    };

    private void updateLightLevelUI(int lux) {
        lightLevelValueText.setText(String.valueOf(lux));
        lightLevelProgress.setProgress(lux);

        // Update the qualitative assessment and status indicator
        if (lux < LUX_DIM_THRESHOLD) {
            lightLevelText.setText("Dim");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_yellow);
        } else if (lux < LUX_NORMAL_THRESHOLD) {
            lightLevelText.setText("Normal");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            lightLevelText.setText("Bright");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        // Update the timestamp
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastUpdatedTimeText.setText(currentTime);
    }
}
