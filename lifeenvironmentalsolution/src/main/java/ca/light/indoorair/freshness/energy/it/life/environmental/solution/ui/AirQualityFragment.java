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

import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.AirQualityViewModel;

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

    // --- ViewModel ---
    private AirQualityViewModel viewModel;

    public AirQualityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_air_quality, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AirQualityViewModel.class);

        initializeViews(view);
        setupControlListeners();
        observeViewModel();

        viewModel.init();
        loadSettings();
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
    }

    private void observeViewModel() {
        viewModel.airQualityValue.observe(getViewLifecycleOwner(), value -> {
            airQualityProgress.setProgress(value);
            airQualityValueText.setText(String.valueOf(value));
        });

        viewModel.airQualityLevel.observe(getViewLifecycleOwner(), level -> {
            airQualityLevelText.setText(level);
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            lastUpdatedTime.setText(time);
        });

        viewModel.statusColor.observe(getViewLifecycleOwner(), colorResId -> {
            if (getContext() != null) {
                int color = ContextCompat.getColor(getContext(), colorResId);
                ((GradientDrawable) statusIndicator.getBackground()).setColor(color);
            }
        });

        viewModel.isRefreshing.observe(getViewLifecycleOwner(), isRefreshing -> {
            buttonRefresh.setEnabled(!isRefreshing);
            if (isRefreshing) {
                Toast.makeText(getContext(), "Refreshing data...", Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.error.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupControlListeners() {
        buttonRefresh.setOnClickListener(v -> viewModel.manualRefresh());

        sliderAlertLevel.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int alertValue = (int) value;
                alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", alertValue));
                viewModel.saveIntSetting(AirQualityViewModel.KEY_ALERT_LEVEL, alertValue);
            }
        });

        switchAutoVentilation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.saveBooleanSetting(AirQualityViewModel.KEY_AUTO_VENT, isChecked);
                Toast.makeText(getContext(), "Auto Ventilation " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        switchPurifierPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                viewModel.saveBooleanSetting(AirQualityViewModel.KEY_PURIFIER_POWER, isChecked);
                sliderPurifierIntensity.setEnabled(isChecked);
                Toast.makeText(getContext(), "Air Purifier " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            }
        });

        sliderPurifierIntensity.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int intensity = (int) value;
                viewModel.saveIntSetting(AirQualityViewModel.KEY_PURIFIER_INTENSITY, intensity);
                Toast.makeText(getContext(), "Purifier intensity set to " + intensity, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSettings() {
        int savedAlertLevel = viewModel.getIntSetting(AirQualityViewModel.KEY_ALERT_LEVEL, 2000);
        sliderAlertLevel.setValue(savedAlertLevel);
        alertLevelLabel.setText(String.format(Locale.US, "Alert Level (%d PPM)", savedAlertLevel));

        boolean autoVentEnabled = viewModel.getBooleanSetting(AirQualityViewModel.KEY_AUTO_VENT, false);
        switchAutoVentilation.setChecked(autoVentEnabled);

        boolean purifierPowerEnabled = viewModel.getBooleanSetting(AirQualityViewModel.KEY_PURIFIER_POWER, false);
        switchPurifierPower.setChecked(purifierPowerEnabled);
        sliderPurifierIntensity.setEnabled(purifierPowerEnabled);

        int purifierIntensity = viewModel.getIntSetting(AirQualityViewModel.KEY_PURIFIER_INTENSITY, 1);
        sliderPurifierIntensity.setValue(purifierIntensity);
    }
}
