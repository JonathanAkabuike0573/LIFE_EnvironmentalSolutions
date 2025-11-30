package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.os.Bundle;
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

public class PresenceFragment extends Fragment {

    // UI Elements
    private TextView presenceStatusText, lastUpdatedTimeText, sessionDurationText, totalDetectionsText, labelAutoOffTime;
    private ImageView presenceIcon;
    private View statusIndicator;
    private SwitchMaterial presenceDetectionSwitch;
    private Button markOccupiedButton, markEmptyButton;
    private CheckBox autoLightsOffCheckbox, alertAfterHoursCheckbox;
    private Spinner autoOffTimeSpinner;
    private LinearLayout timeRangePickerContainer;
    private TextInputEditText startTimeInput, endTimeInput;

    // ViewModel
    private PresenceViewModel viewModel;

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

        viewModel = new ViewModelProvider(this).get(PresenceViewModel.class);

        initializeViews(view);
        setupListeners();
        setupSpinner();
        observeViewModel();

        viewModel.init();
    }

    private void initializeViews(View view) {
        presenceStatusText = view.findViewById(R.id.presence_status_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
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

    private void setupListeners() {
        presenceDetectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.setPresenceDetectionEnabled(isChecked);
                Toast.makeText(getContext(), isChecked ? "Presence detection enabled" : "Presence detection disabled", Toast.LENGTH_SHORT).show();
            }
        });

        autoLightsOffCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            labelAutoOffTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            autoOffTimeSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        alertAfterHoursCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setAlertsEnabled(isChecked);
            timeRangePickerContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        startTimeInput.setOnClickListener(v -> showTimePicker(true));
        endTimeInput.setOnClickListener(v -> showTimePicker(false));

        markOccupiedButton.setOnClickListener(v -> viewModel.manualOverride("Occupied"));
        markEmptyButton.setOnClickListener(v -> viewModel.manualOverride("Empty"));
    }

    private void observeViewModel() {
        viewModel.presenceDetectionEnabled.observe(getViewLifecycleOwner(), isEnabled -> {
            presenceDetectionSwitch.setChecked(isEnabled);
        });

        viewModel.presenceStatus.observe(getViewLifecycleOwner(), status -> {
            updatePresenceUI(status);
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            lastUpdatedTimeText.setText(time);
        });

        viewModel.sessionDuration.observe(getViewLifecycleOwner(), duration -> {
            if (duration != null) {
                sessionDurationText.setText(String.format(Locale.getDefault(), "%d seconds", duration));
            } else {
                sessionDurationText.setText("--");
            }
        });

        viewModel.totalDetections.observe(getViewLifecycleOwner(), detections -> {
            if (detections != null) {
                totalDetectionsText.setText(String.valueOf(detections));
            } else {
                totalDetectionsText.setText("--");
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updatePresenceUI(String status) {
        boolean occupied = "occupied".equalsIgnoreCase(status);

        if (occupied) {
            presenceStatusText.setText(R.string.occupied);
            presenceIcon.setImageResource(R.drawable.ic_room_occupied);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_green);
        } else {
            presenceStatusText.setText(R.string.empty);
            presenceIcon.setImageResource(R.drawable.ic_room_empty);
            statusIndicator.setBackgroundResource(R.drawable.circle_indicator_red);
        }
    }

    private void showTimePicker(boolean isStartTime) {
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
            if (isStartTime) {
                startTimeInput.setText(formattedTime);
                viewModel.setAlertStartTime(formattedTime);
            } else {
                endTimeInput.setText(formattedTime);
                viewModel.setAlertEndTime(formattedTime);
            }
        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.auto_off_time_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        autoOffTimeSpinner.setAdapter(adapter);
    }
}
