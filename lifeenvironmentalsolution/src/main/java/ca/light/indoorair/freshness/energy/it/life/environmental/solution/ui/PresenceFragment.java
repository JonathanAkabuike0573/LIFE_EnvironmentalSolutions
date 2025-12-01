package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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

    public PresenceFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_presence, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PresenceViewModel.class);
        sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);

        initializeViews(view);
        setupRoomSynchronization();
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
    }

    private void setupRoomSynchronization() {
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty()) {
                if (currentRoomText != null) currentRoomText.setText(roomName);
                if (viewModel != null) viewModel.init(roomName);
            }
        });
    }

    private void setupSpinner() {
        if (autoOffTimeSpinner == null || getContext() == null) return;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(), R.array.auto_off_time_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoOffTimeSpinner.setAdapter(adapter);
    }

    private void setupListeners() {
        if (presenceDetectionSwitch != null) {
            presenceDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && viewModel != null) {
                    viewModel.setPresenceDetectionEnabled(isChecked);
                    safeToast(isChecked ? "Presence detection enabled" : "Presence detection disabled");
                }
            });
        }

        if (autoLightsOffCheckbox != null) {
            autoLightsOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                boolean visible = isChecked;
                if (labelAutoOffTime != null) labelAutoOffTime.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (autoOffTimeSpinner != null) autoOffTimeSpinner.setVisibility(visible ? View.VISIBLE : View.GONE);

                if (viewModel != null) {
                    if (isChecked) {
                        String timeoutText = autoOffTimeSpinner.getSelectedItem() != null ?
                                autoOffTimeSpinner.getSelectedItem().toString() : "5 minutes";
                        viewModel.setAutoLightsOffEnabled(true, parseTimeoutMinutes(timeoutText));
                    } else {
                        viewModel.setAutoLightsOffEnabled(false, 0);
                    }
                }
            });

            if (autoOffTimeSpinner != null) {
                autoOffTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (autoLightsOffCheckbox.isChecked() && viewModel != null) {
                            String timeoutText = parent.getItemAtPosition(position).toString();
                            viewModel.setAutoLightsOffEnabled(true, parseTimeoutMinutes(timeoutText));
                        }
                    }
                    @Override public void onNothingSelected(AdapterView<?> parent) {}
                });
            }
        }

        if (alertAfterHoursCheckbox != null) {
            alertAfterHoursCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (viewModel != null) viewModel.setAlertsEnabled(isChecked);
                if (timeRangePickerContainer != null) timeRangePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }

        if (startTimeInput != null) startTimeInput.setOnClickListener(v -> showTimePicker(true));
        if (endTimeInput != null) endTimeInput.setOnClickListener(v -> showTimePicker(false));

        if (markOccupiedButton != null && viewModel != null) {
            markOccupiedButton.setOnClickListener(v -> viewModel.manualOverride("Occupied"));
        }
        if (markEmptyButton != null && viewModel != null) {
            markEmptyButton.setOnClickListener(v -> viewModel.manualOverride("Empty"));
        }
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        viewModel.presenceDetectionEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            if (presenceDetectionSwitch != null) presenceDetectionSwitch.setChecked(isEnabled != null && isEnabled);
        });

        viewModel.autoLightsOffEnabled.observe(getViewLifecycleOwner(), enabled -> {
            if (autoLightsOffCheckbox != null) {
                autoLightsOffCheckbox.setChecked(enabled != null && enabled);
                boolean visible = enabled != null && enabled;
                if (labelAutoOffTime != null) labelAutoOffTime.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (autoOffTimeSpinner != null) autoOffTimeSpinner.setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.presenceStatus.observe(getViewLifecycleOwner(), status -> {
            if (status != null) updatePresenceUI(status);
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            if (lastUpdatedTimeText != null && time != null) lastUpdatedTimeText.setText(time);
        });

        viewModel.sessionDuration.observe(getViewLifecycleOwner(), duration -> {
            if (sessionDurationText != null) sessionDurationText.setText(duration != null ? duration + " seconds" : "--");
        });

        viewModel.totalDetections.observe(getViewLifecycleOwner(), detections -> {
            if (totalDetectionsText != null) totalDetectionsText.setText(detections != null ? String.valueOf(detections) : "--");
        });

        // --- NEW: Toast Timer Events ---
        viewModel.timerEventMessage.observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                safeToast(message);
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) safeToast(error);
        });
    }

    private void updatePresenceUI(String status) {
        if (presenceStatusText == null) return;
        boolean occupied = "occupied".equalsIgnoreCase(status);
        presenceStatusText.setText(occupied ? "Occupied" : "Vacant");
        if (presenceIcon != null) presenceIcon.setImageResource(occupied ? R.drawable.ic_room_occupied : R.drawable.ic_room_empty);
        if (statusIndicator != null) statusIndicator.setBackgroundResource(occupied ? R.drawable.circle_indicator_green : R.drawable.circle_indicator_red);
    }

    private void showTimePicker(boolean isStartTime) {
        if (getParentFragmentManager() == null) return;
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H).setHour(12).setMinute(0).setTitleText("Select Time").build();
        picker.addOnPositiveButtonClickListener(v -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            if (isStartTime) { if (startTimeInput != null) startTimeInput.setText(time); viewModel.setAlertStartTime(time); }
            else { if (endTimeInput != null) endTimeInput.setText(time); viewModel.setAlertEndTime(time); }
        });
        picker.show(getParentFragmentManager(), "timePicker");
    }

    private int parseTimeoutMinutes(String text) {
        switch (text) {
            case "1 minute": return 1;
            case "2 minutes": return 2;
            case "5 minutes": return 5;
            case "10 minutes": return 10;
            case "15 minutes": return 15;
            default: return 5;
        }
    }

    private void safeToast(String message) {
        if (getContext() != null) Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
