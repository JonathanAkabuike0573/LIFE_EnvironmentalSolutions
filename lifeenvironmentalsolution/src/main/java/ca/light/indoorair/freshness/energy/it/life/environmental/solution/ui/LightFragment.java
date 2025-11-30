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
import androidx.cardview.widget.CardView;
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

    // UI Elements
    private TextView lightLevelValueText, lightLevelText, lastUpdatedTimeText, currentRoomText;
    private ProgressBar lightLevelProgress;
    private View statusIndicator;
    private CardView lightLevelCard;
    private ChipGroup lightBrightnessChipGroup, lightPresetsChipGroup;
    private SwitchMaterial autoBrightnessSwitch;
    private Slider brightnessSlider;

    // ViewModels
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
        currentRoomText = view.findViewById(R.id.current_room_text);
        lightLevelProgress = view.findViewById(R.id.light_level_progress);
        statusIndicator = view.findViewById(R.id.status_indicator);
        lightLevelCard = view.findViewById(R.id.light_level_card);
        lightBrightnessChipGroup = view.findViewById(R.id.chip_group_light_brightness);
        autoBrightnessSwitch = view.findViewById(R.id.power_on);
        brightnessSlider = view.findViewById(R.id.slider_brightness);
        lightPresetsChipGroup = view.findViewById(R.id.chip_group_presets);
    }

    private void setupRoomSync() {
        sharedRoomViewModel.getCurrentRoom().observe(getViewLifecycleOwner(), roomName -> {
            if (roomName != null && !roomName.isEmpty() && currentRoomText != null) {
                currentRoomText.setText(roomName);
                viewModel.init(roomName);
                safeToast("Light: Switched to " + roomName);
            }
        });
    }

    private void setupListeners() {

        if (lightBrightnessChipGroup != null) {
            lightBrightnessChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int checkedId = checkedIds.get(0);
                    Chip selectedChip = group.findViewById(checkedId);
                    if (selectedChip != null) {
                        viewModel.setBrightness(selectedChip.getText().toString());
                        safeToast(selectedChip.getText() + " selected");
                    }
                }
            });
        }


        if (lightPresetsChipGroup != null) {
            lightPresetsChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int checkedId = checkedIds.get(0);
                    Chip selectedChip = group.findViewById(checkedId);
                    if (selectedChip != null) {
                        viewModel.setPreset(selectedChip.getText().toString());
                        safeToast("Mode: " + selectedChip.getText());
                    }
                }
            });
        }


        if (autoBrightnessSwitch != null) {
            autoBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    viewModel.setAutoBrightness(isChecked);
                    if (brightnessSlider != null) {
                        brightnessSlider.setEnabled(!isChecked);
                    }
                    safeToast(isChecked ? "Auto brightness enabled" : "Auto brightness disabled");
                }
            });
        }


        if (brightnessSlider != null) {
            brightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
                if (fromUser) {
                    viewModel.setSliderBrightness((int) value);
                }
            });
        }
    }

    private void observeViewModel() {
        viewModel.lux.observe(getViewLifecycleOwner(), lux -> {
            if (lux != null && lightLevelValueText != null && lightLevelProgress != null) {
                lightLevelValueText.setText(String.valueOf(lux));
                lightLevelProgress.setProgress(lux);
            }
        });

        viewModel.lightLevelText.observe(getViewLifecycleOwner(), text -> {
            if (text != null && lightLevelText != null) lightLevelText.setText(text);
        });

        viewModel.lastUpdatedTime.observe(getViewLifecycleOwner(), time -> {
            if (time != null && lastUpdatedTimeText != null) lastUpdatedTimeText.setText(time);
        });

        viewModel.statusColor.observe(getViewLifecycleOwner(), color -> {
            if (color != null && statusIndicator != null) {
                statusIndicator.setBackgroundResource(color);
            }
        });


        viewModel.cardGlowColor.observe(getViewLifecycleOwner(), color -> {
            if (color != null && lightLevelCard != null) {
                lightLevelCard.setCardBackgroundColor(
                        getResources().getColor(color, getContext().getTheme())
                );
            }
        });


        viewModel.sliderPosition.observe(getViewLifecycleOwner(), position -> {
            if (position != null && brightnessSlider != null) {
                brightnessSlider.setValue(position);
            }
        });

        viewModel.brightness.observe(getViewLifecycleOwner(), brightness -> {
            if (brightness != null) updateBrightnessSelection(brightness);
        });

        viewModel.autoBrightness.observe(getViewLifecycleOwner(), enabled -> {
            if (enabled != null) updateAutoBrightnessState(enabled);
        });
    }

    private void updateBrightnessSelection(String brightness) {
        if (lightBrightnessChipGroup == null) return;
        int chipId = R.id.chip_neutral;
        if ("Warm".equals(brightness)) chipId = R.id.chip_warm;
        else if ("Neutral".equals(brightness)) chipId = R.id.chip_neutral;
        else if ("Cool".equals(brightness)) chipId = R.id.chip_cool;
        lightBrightnessChipGroup.check(chipId);
    }

    private void updateAutoBrightnessState(boolean enabled) {
        if (autoBrightnessSwitch != null) autoBrightnessSwitch.setChecked(enabled);
        if (brightnessSlider != null) brightnessSlider.setEnabled(!enabled);
    }

    private void safeToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
