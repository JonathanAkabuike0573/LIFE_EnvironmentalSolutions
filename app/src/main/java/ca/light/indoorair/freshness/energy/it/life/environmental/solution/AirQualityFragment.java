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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class AirQualityFragment extends Fragment {

    // UI Elements
    private TextView airQualityValueText, airQualityLevelText, lastUpdatedTimeText;
    private ProgressBar airQualityProgress;
    private View statusIndicator;

    // Simulation logic
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private static final int SIMULATION_INTERVAL = 5000; // 5 seconds

    // Air quality thresholds
    private static final int PPM_GOOD_THRESHOLD = 1000;
    private static final int PPM_MODERATE_THRESHOLD = 2000;

    public AirQualityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_air_quality, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
    }

    private void initializeViews(View view) {
        airQualityValueText = view.findViewById(R.id.air_quality_value_text);
        airQualityLevelText = view.findViewById(R.id.air_quality_level_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        airQualityProgress = view.findViewById(R.id.air_quality_progress);
        statusIndicator = view.findViewById(R.id.status_indicator);
    }

    @Override
    public void onResume() {
        super.onResume();
        startAirQualitySimulation();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAirQualitySimulation();
    }

    private void startAirQualitySimulation() {
        simulationHandler.post(airQualityRunnable);
    }

    private void stopAirQualitySimulation() {
        simulationHandler.removeCallbacks(airQualityRunnable);
    }

    private final Runnable airQualityRunnable = new Runnable() {
        @Override
        public void run() {
            // Simulate a new air quality reading (fluctuating around a baseline)
            int currentPpm = 400 + random.nextInt(2100); // Ranges from 400 to 2500
            updateAirQualityUI(currentPpm);

            // Schedule the next update
            simulationHandler.postDelayed(this, SIMULATION_INTERVAL);
        }
    };

    private void updateAirQualityUI(int ppm) {
        airQualityValueText.setText(String.valueOf(ppm));
        airQualityProgress.setProgress(ppm);

        // Update the qualitative assessment and status indicator
        if (ppm < PPM_GOOD_THRESHOLD) {
            airQualityLevelText.setText("Good");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else if (ppm < PPM_MODERATE_THRESHOLD) {
            airQualityLevelText.setText("Moderate");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_yellow);
        } else {
            airQualityLevelText.setText("Poor");
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        // Update the timestamp
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastUpdatedTimeText.setText(currentTime);
    }
}
