package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.LightViewModel;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.SharedRoomViewModel;

public class LightFragment extends Fragment {

    private TextView lightLevelValueText, lightLevelText, lastUpdatedTimeText, currentRoomText;
    private ProgressBar lightLevelProgress;
    private View statusIndicator;
    private ChipGroup lightBrightnessChipGroup;
    private SwitchMaterial autoBrightnessSwitch;
    private Slider brightnessSlider;

    private LightViewModel viewModel;
    private SharedRoomViewModel sharedRoomViewModel;

    public LightFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_light, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LightViewModel.class);
        sharedRoomViewModel = new ViewModelProvider(requireActivity()).get(SharedRoomViewModel.class);

        initializeViews(view);
        setupRoomSync();
        setupListeners();
        observeViewModel();
    }

    private void initializeViews(View view) {
        lightLevelValueText = view.findViewById(R.id.light_level_value_text);
        lightLevelText = view.findViewById(R.id.light_level_text);
        lastUpdatedTimeText = view.findViewById(R.id.last_updated_time);
        currentRoomText = view.findViewById(R.id.current_room_text); // Add this to layout
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        statusIndicator = view.findViewById(R.id.status_indicator);
        lightBrightnessChipGroup = view.findViewById(R.id.chip_group_light_brightness);
        autoBrightnessSwitch = view.findViewById(R.id.power_on);
        brightnessSlider = view.findViewById(R.id.slider_brightness);
    }

    private void setupRoomSync() {
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty() && currentRoomText != null) {
                currentRoomText.setText(roomName);
                viewModel.init(roomName);
            }
        });
    }

    private void setupListeners() {
        lightBrightnessChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip != null) {
                viewModel.setBrightness(selectedChip.getText().toString());
                Toast.makeText(getContext(), selectedChip.getText() + " selected", Toast.LENGTH_SHORT).show();
            }
        });

        if (autoBrightnessSwitch != null) {
            autoBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    viewModel.setAutoBrightness(isChecked);
                    brightnessSlider.setEnabled(!isChecked);
                    Toast.makeText(getContext(), isChecked ? "Auto brightness enabled" : "Auto brightness disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void observeViewModel() {
        viewModel.lux.observe(getViewLifecycleOwner(), lux -> {
            if (lux != null) {
                lightLevelValueText.setText(String.valueOf(lux));
                lightLevelProgress.setProgress(lux);
            }
        });

        viewModel.lightLevelText.observe(getViewLifecycleOwner(), text -> {
            if (text != null) lightLevelText.setText(text);
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            if (time != null) lastUpdatedTimeText.setText(time);
        });

        viewModel.statusColor.observe(getViewLifecycleOwner(), color -> {
            if (color != null && statusIndicator != null) {
                statusIndicator.setBackgroundResource(color);
            }
        });

        viewModel.brightness.observe(getViewLifecycleOwner(), brightness -> {
            if (brightness != null) {
                updateBrightnessSelection(brightness);
            }
        });

        viewModel.autoBrightness.observe(getViewLifecycleOwner(), enabled -> {
            if (enabled != null && autoBrightnessSwitch != null) {
                autoBrightnessSwitch.setChecked(enabled);
                if (brightnessSlider != null) {
                    brightnessSlider.setEnabled(!enabled);
                }
            }
        });
    }

    private void updateBrightnessSelection(String brightness) {
        int chipId = R.id.chip_neutral;
        if ("Warm".equals(brightness)) chipId = R.id.chip_warm;
        else if ("Neutral".equals(brightness)) chipId = R.id.chip_neutral;
        else if ("Cool".equals(brightness)) chipId = R.id.chip_cool;

        lightBrightnessChipGroup.check(chipId);
    }
}
