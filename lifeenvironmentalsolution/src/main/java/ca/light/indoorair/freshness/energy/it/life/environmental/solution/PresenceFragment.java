package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PresenceFragment extends Fragment {

    // UI Elements
    private TextView presenceStatusText, lastUpdatedTimeText;
    private ImageView presenceIcon;
    private View statusIndicator;

    // Simulation logic
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private boolean isOccupied = false;

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
        initializeViews(view);
    }

    private void initializeViews(View view) {
        presenceStatusText = view.findViewById(R.id.presence_status_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        presenceIcon = view.findViewById(R.id.presence_icon);
        statusIndicator = view.findViewById(R.id.status_indicator);
    }

    @Override
    public void onResume() {
        super.onResume();
        startPresenceSimulation();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopPresenceSimulation();
    }

    private void startPresenceSimulation() {
        simulationHandler.post(presenceRunnable);
    }

    private void stopPresenceSimulation() {
        simulationHandler.removeCallbacks(presenceRunnable);
    }

    private final Runnable presenceRunnable = new Runnable() {
        @Override
        public void run() {
            // Toggle the presence state
            isOccupied = !isOccupied;
            updatePresenceUI(isOccupied);

            // Schedule the next update with a random delay to make it feel more real
            int randomDelay = 5000 + random.nextInt(5000); // 5 to 10 seconds
            simulationHandler.postDelayed(this, randomDelay);
        }
    };

    private void updatePresenceUI(boolean occupied) {
        if (occupied) {
            presenceStatusText.setText("Occupied");
            presenceIcon.setImageResource(R.drawable.ic_room_occupied);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            presenceStatusText.setText("Empty");
            presenceIcon.setImageResource(R.drawable.ic_room_empty);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        // Update the timestamp
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        lastUpdatedTimeText.setText(currentTime);
    }
}
