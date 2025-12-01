package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.AirQualityViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.SharedRoomViewModel;

public class AirQualityFragment extends Fragment {

    // --- UI Views ---
    private ProgressBar airQualityProgress;
    private TextView airQualityValueText;
    private TextView airQualityLevelText;
    private TextView lastUpdatedTime;
    private View statusIndicator;
    private MaterialButton buttonRefresh;
    private Slider sliderAlertLevel;
    private TextView alertLevelLabel;
    private SwitchMaterial switchAutoVentilation;
    private SwitchMaterial switchPurifierPower;
    private Slider sliderPurifierIntensity;
    private TextView roomNameText;

    //  ViewModel
    private AirQualityViewModel viewModel;
    private String currentRoom;

    public AirQualityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_air_quality, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        viewModel = new ViewModelProvider(this).get(AirQualityViewModel.class);


        SharedRoomViewModel sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);


        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty()) {
                currentRoom = roomName;
                roomNameText.setText(roomName);


                viewModel.init(roomName);


                Toast.makeText(getContext(), "Switched to: " + roomName, Toast.LENGTH_SHORT).show();
            }
        });

        setupControlListeners();
        observeViewModel();
        loadSettings();
    }


    private String getSelectedRoomFromArguments() {
        Bundle args = getArguments();
        if (args != null && args.containsKey("SELECTED_ROOM_KEY")) {
            return args.getString("SELECTED_ROOM_KEY", "Main Office");
        }
        return "Main Office";
    }

    @Override
    public void onResume() {
        super.onResume();
        // Log that the user is viewing the Air Quality Screen
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(requireContext());
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "Air Quality Screen");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AirQualityFragment");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    private void initializeViews(View view) {
        airQualityProgress = view.findViewById(R.id.air_quality_progress);
        airQualityValueText = view.findViewById(R.id.air_quality_value_text);
        airQualityLevelText = view.findViewById(R.id.air_quality_level_text);
        lastUpdatedTime = view.findViewById(R.id.last_updated_time);
        statusIndicator = view.findViewById(R.id.status_indicator);
        alertLevelLabel = view.findViewById(R.id.alert_level_label);
        buttonRefresh = view.findViewById(R.id.button_refresh);
        sliderAlertLevel = view.findViewById(R.id.slider_alert_level);
        switchAutoVentilation = view.findViewById(R.id.switch_auto_ventilation);
        switchPurifierPower = view.findViewById(R.id.switch_purifier_power);
        sliderPurifierIntensity = view.findViewById(R.id.slider_purifier_intensity);
        roomNameText = view.findViewById(R.id.room_name_text);
    }

    private void observeViewModel() {
        viewModel.airQualityValue.observe(getViewLifecycleOwner(), value -> {
            if (value != null && airQualityProgress != null && airQualityValueText != null) {
                airQualityProgress.setProgress(value);
                airQualityValueText.setText(String.valueOf(value));
            }
        });

        viewModel.airQualityLevel.observe(getViewLifecycleOwner(), level -> {
            if (level != null && airQualityLevelText != null) {
                airQualityLevelText.setText(level);
            }
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            if (time != null && lastUpdatedTime != null) {
                lastUpdatedTime.setText(time);
            }
        });

        viewModel.statusColor.observe(getViewLifecycleOwner(), colorResId -> {
            if (getContext() != null && colorResId != null && statusIndicator != null) {
                try {
                    int color = ContextCompat.getColor(getContext(), colorResId);
                    if (statusIndicator.getBackground() instanceof GradientDrawable) {
                        ((GradientDrawable) statusIndicator.getBackground()).setColor(color);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        viewModel.isRefreshing.observe(getViewLifecycleOwner(), isRefreshing -> {
            if (buttonRefresh != null) {
                buttonRefresh.setEnabled(!Boolean.TRUE.equals(isRefreshing));
            }
            if (Boolean.TRUE.equals(isRefreshing)) {
                Toast.makeText(getContext(), "Refreshing " + currentRoom + " data...", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupControlListeners() {
        if (buttonRefresh != null) {
            buttonRefresh.setOnClickListener(v -> viewModel.manualRefresh());
        }

        if (sliderAlertLevel != null) {
            sliderAlertLevel.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser && alertLevelLabel != null) {
                    int alertValue = (int) value;
                    alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", alertValue));
                    viewModel.saveIntSetting(AirQualityViewModel.KEY_ALERT_LEVEL, alertValue);
                }
            });
        }

        if (switchAutoVentilation != null) {
            switchAutoVentilation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    viewModel.saveBooleanSetting(AirQualityViewModel.KEY_AUTO_VENT, isChecked);
                    Toast.makeText(getContext(), "Auto Ventilation " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchPurifierPower != null) {
            switchPurifierPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed() && sliderPurifierIntensity != null) {
                    viewModel.saveBooleanSetting(AirQualityViewModel.KEY_PURIFIER_POWER, isChecked);
                    sliderPurifierIntensity.setEnabled(isChecked);
                    Toast.makeText(getContext(), "Air Purifier " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (sliderPurifierIntensity != null) {
            sliderPurifierIntensity.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    int intensity = (int) value;
                    viewModel.saveIntSetting(AirQualityViewModel.KEY_PURIFIER_INTENSITY, intensity);
                    Toast.makeText(getContext(), "Purifier intensity set to " + intensity, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadSettings() {
        if (sliderAlertLevel != null && alertLevelLabel != null) {
            int savedAlertLevel = viewModel.getIntSetting(AirQualityViewModel.KEY_ALERT_LEVEL, 2000);
            sliderAlertLevel.setValue(savedAlertLevel);
            alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", savedAlertLevel));
        }

        if (switchAutoVentilation != null) {
            boolean autoVentEnabled = viewModel.getBooleanSetting(AirQualityViewModel.KEY_AUTO_VENT, false);
            switchAutoVentilation.setChecked(autoVentEnabled);
        }

        if (switchPurifierPower != null && sliderPurifierIntensity != null) {
            boolean purifierPowerEnabled = viewModel.getBooleanSetting(AirQualityViewModel.KEY_PURIFIER_POWER, false);
            switchPurifierPower.setChecked(purifierPowerEnabled);
            sliderPurifierIntensity.setEnabled(purifierPowerEnabled);
        }

        if (sliderPurifierIntensity != null) {
            int purifierIntensity = viewModel.getIntSetting(AirQualityViewModel.KEY_PURIFIER_INTENSITY, 1);
            sliderPurifierIntensity.setValue(purifierIntensity);
        }
    }

    public static AirQualityFragment newInstance(String selectedRoom) {
        AirQualityFragment fragment = new AirQualityFragment();
        Bundle args = new Bundle();
        args.putString("SELECTED_ROOM_KEY", selectedRoom);
        fragment.setArguments(args);
        return fragment;
    }
}