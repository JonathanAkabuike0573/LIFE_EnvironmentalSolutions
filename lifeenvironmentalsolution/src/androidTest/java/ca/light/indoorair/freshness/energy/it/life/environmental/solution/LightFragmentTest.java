package ca.light.indoorair.freshness.energy.it.life.environmental.solution;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.LightFragment;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 36)
public class LightFragmentTest {

    @Test
    public void powerSwitch_click_showsToast() {
        try (FragmentScenario<LightFragment> scenario = 
                FragmentScenario.launchInContainer(
                        LightFragment.class,
                        null, 
                        R.style.Theme_LIFE_EnvironmentalSolution,
                        (FragmentFactory) null
                )) {
            scenario.onFragment(fragment -> {
                SwitchMaterial powerSwitch = 
                        fragment.requireView().findViewById(R.id.light_switch);

                // When user toggles the power switch
                powerSwitch.performClick();

                // Then a toast with the expected text is shown
                String latestToast = ShadowToast.getTextOfLatestToast();
                // Adjust the expected text if your string changes
                assertEquals("Light turned on", latestToast);
            });
        }
    }

    @Test
    public void autoBrightness_enabled_disablesSlider() {
        try (FragmentScenario<LightFragment> scenario = 
                FragmentScenario.launchInContainer(
                        LightFragment.class,
                        null, 
                        R.style.Theme_LIFE_EnvironmentalSolution,
                        (FragmentFactory) null
                )) {
            scenario.onFragment(fragment -> {
                SwitchMaterial autoSwitch = 
                        fragment.requireView().findViewById(R.id.power_on);
                Slider brightnessSlider = 
                        fragment.requireView().findViewById(R.id.slider_brightness);

                // Ensure slider starts enabled (may depend on your initial state)
                brightnessSlider.setEnabled(true);

                // When auto‑brightness is toggled on
                autoSwitch.performClick();

                // Then manual brightness slider is disabled
                assertFalse(brightnessSlider.isEnabled());
            });
        }
    }
}
