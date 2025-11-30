package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.os.Bundle;
import android.util.Log;
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.PresenceViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.SharedRoomViewModel;

public class PresenceFragment extends Fragment {

    // UI Elements
    private TextView presenceStatusText, lastUpdatedTimeText, sessionDurationText,
            totalDetectionsText, labelAutoOffTime, currentRoomText;
    private ImageView presenceIcon;
    private View statusIndicator;
    private SwitchMaterial presenceDetectionSwitch;
    private Button markOccupiedButton, markEmptyButton;
    private CheckBox autoLightsOffCheckbox, alertAfterHoursCheckbox;
    private Spinner autoOffTimeSpinner;
    private LinearLayout timeRangePickerContainer;
    private TextInputEditText startTimeInput, endTimeInput;

    // ViewModels
    private PresenceViewModel viewModel;
    private SharedRoomViewModel sharedRoomViewModel;

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

        // 1. Initialize ViewModels FIRST
        viewModel = new ViewModelProvider(this).get(PresenceViewModel.class);
        sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);

        // 2. Initialize ALL views
        initializeViews(view);

        // 3. Setup room synchronization (SAFE)
        setupRoomSynchronization();

        // 4. Setup UI components
        setupSpinner();
        setupListeners();
        observeViewModel();
    }

    private void initializeViews(View view) {
        presenceStatusText = view.findViewById(R.id.presence_status_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        currentRoomText = view.findViewById(R.id.current_room_text);
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

        Log.d("PresenceFragment", "Views initialized. currentRoomText: " + (currentRoomText != null ? "OK" : "NULL"));
    }

    private void setupRoomSynchronization() {
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty()) {
                // SAFE TextView update
                if (currentRoomText != null) {
                    currentRoomText.setText(roomName);
                }

                // Initialize ViewModel with room
                if (viewModel != null) {
                    viewModel.init(roomName);
                }

                // Safe toast
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Presence: Switched to " + roomName, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupListeners() {
        // Presence detection switch
        if (presenceDetectionSwitch != null) {
            presenceDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && viewModel != null) {
                    viewModel.setPresenceDetectionEnabled(isChecked);
                    safeToast(isChecked ? "Presence detection enabled" : "Presence detection disabled");
                }
            });
        }

        // Auto lights off
        if (autoLightsOffCheckbox != null && labelAutoOffTime != null && autoOffTimeSpinner != null) {
            autoLightsOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                labelAutoOffTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                autoOffTimeSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        // Alerts checkbox
        if (alertAfterHoursCheckbox != null && timeRangePickerContainer != null) {
            alertAfterHoursCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (viewModel != null) {
                    viewModel.setAlertsEnabled(isChecked);
                }
                timeRangePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        // Time pickers
        if (startTimeInput != null) {
            startTimeInput.setOnClickListener(v -> showTimePicker(true));
        }
        if (endTimeInput != null) {
            endTimeInput.setOnClickListener(v -> showTimePicker(false));
        }

        // Manual override buttons
        if (markOccupiedButton != null && viewModel != null) {
            markOccupiedButton.setOnClickListener(v -> viewModel.manualOverride("Occupied"));
        }
        if (markEmptyButton != null && viewModel != null) {
            markEmptyButton.setOnClickListener(v -> viewModel.manualOverride("Empty"));
        }
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        // Presence detection enabled
        viewModel.presenceDetectionEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            if (presenceDetectionSwitch != null) {
                presenceDetectionSwitch.setChecked(isEnabled != null && isEnabled);
            }
        });

        // Presence status
        viewModel.presenceStatus.observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                updatePresenceUI(status);
            }
        });

        // Timestamps and counters
        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            if (lastUpdatedTimeText != null && time != null) {
                lastUpdatedTimeText.setText(time);
            }
        });

        viewModel.sessionDuration.observe(getViewLifecycleOwner(), duration -> {
            if (sessionDurationText != null) {
                if (duration != null) {
                    sessionDurationText.setText(String.format(Locale.getDefault(), "%d seconds", duration));
                } else {
                    sessionDurationText.setText("--");
                }
            }
        });

        viewModel.totalDetections.observe(getViewLifecycleOwner(), detections -> {
            if (totalDetectionsText != null) {
                if (detections != null) {
                    totalDetectionsText.setText(String.valueOf(detections));
                } else {
                    totalDetectionsText.setText("--");
                }
            }
        });

        // Errors
        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                safeToast(error);
            }
        });
    }

    private void updatePresenceUI(String status) {
        if (presenceStatusText == null || presenceIcon == null || statusIndicator == null) return;

        boolean occupied = "occupied".equalsIgnoreCase(status);

        if (occupied) {
            presenceStatusText.setText("Occupied");
            if (presenceIcon != null) presenceIcon.setImageResource(R.drawable.ic_room_occupied);
            if (statusIndicator != null) statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            presenceStatusText.setText("Vacant");
            if (presenceIcon != null) presenceIcon.setImageResource(R.drawable.ic_room_empty);
            if (statusIndicator != null) statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }
    }

    private void showTimePicker(boolean isStartTime) {
        if (getParentFragmentManager() == null) return;

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

            if (isStartTime && startTimeInput != null && viewModel != null) {
                startTimeInput.setText(formattedTime);
                viewModel.setAlertStartTime(formattedTime);
            } else if (endTimeInput != null && viewModel != null) {
                endTimeInput.setText(formattedTime);
                viewModel.setAlertEndTime(formattedTime);
            }
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    private void setupSpinner() {
        if (autoOffTimeSpinner == null || getContext() == null) return;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.auto_off_time_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoOffTimeSpinner.setAdapter(adapter);
    }

    private void safeToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
