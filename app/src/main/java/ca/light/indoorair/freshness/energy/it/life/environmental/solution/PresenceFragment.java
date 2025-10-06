package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PresenceFragment extends Fragment {

    private TextView presenceStatusTextView;

    public PresenceFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_presence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the TextView from the layout using its ID
        presenceStatusTextView = view.findViewById(R.id.presence_status_text);

        // Set the initial text to indicate it's waiting for data
        initializeTextView();
    }

    /**
     * Sets the initial state of the TextView. In a real app, a listener for the
     * actual data source (e.g., Firebase) would be set up here.
     */
    private void initializeTextView() {
        // Set a clear message that no data is available yet.
        presenceStatusTextView.setText("Waiting for sensor data...");
    }
}
