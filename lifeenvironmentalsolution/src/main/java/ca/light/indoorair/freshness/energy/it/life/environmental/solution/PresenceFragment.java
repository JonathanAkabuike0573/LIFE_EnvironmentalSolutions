package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PresenceFragment extends Fragment {

    // UI Elements
    private TextView presenceStatusText, lastUpdatedTimeText;
    private ImageView presenceIcon;
    private View statusIndicator;
    private SwitchMaterial presenceDetectionSwitch;

    // Firebase
    private DatabaseReference presenceRef;
    private ValueEventListener presenceListener;

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
        initializeFirebase();
        setupSwitchListener();

        if (presenceDetectionSwitch.isChecked()) {
            startListeningForPresence();
        } else {
            resetUI();
        }
    }

    private void initializeViews(View view) {
        presenceStatusText = view.findViewById(R.id.presence_status_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        presenceIcon = view.findViewById(R.id.presence_icon);
        statusIndicator = view.findViewById(R.id.status_indicator);
        presenceDetectionSwitch = view.findViewById(R.id.switch_presence_detection);
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
                    updatePresenceUI(roomStatus, timestamp);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.firebase_error) + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
                resetUI();
            }
        };
    }

    private void setupSwitchListener() {
        presenceDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startListeningForPresence();
                Toast.makeText(getContext(), R.string.presence_detection_enabled, Toast.LENGTH_SHORT).show();
            } else {
                stopListeningForPresence();
                Toast.makeText(getContext(), R.string.presence_detection_disabled, Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public void onStart() {
        super.onStart();
        if (presenceDetectionSwitch != null && presenceDetectionSwitch.isChecked()) {
            startListeningForPresence();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (presenceRef != null && presenceListener != null) {
            presenceRef.removeEventListener(presenceListener);
        }
    }

    @SuppressLint("SetTextI18n")
    private void resetUI() {
        presenceStatusText.setText(R.string.not_monitoring );
        lastUpdatedTimeText.setText(R.string.lastupdatedtime );
        presenceIcon.setImageResource(android.R.color.transparent); // Blank screen
        statusIndicator.setBackgroundResource(R.drawable.circle_indicator_gray);
    }

    private void updatePresenceUI(String status, String timestamp) {
        boolean occupied = getString(R.string.occupied).equalsIgnoreCase(status);

        if (occupied) {
            presenceStatusText.setText(R.string.occupied );
            presenceIcon.setImageResource(R.drawable.ic_room_occupied);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            presenceStatusText.setText(R.string.empty );
            presenceIcon.setImageResource(R.drawable.ic_room_empty);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }

        if (timestamp != null) {
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
        } else {
            lastUpdatedTimeText.setText(R.string.n_a );
        }
    }
}
