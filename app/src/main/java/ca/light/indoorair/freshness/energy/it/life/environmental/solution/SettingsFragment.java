//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    // Define SharedPreferences constants for easy access and to avoid typos
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String PORTRAIT_LOCK_KEY = "PortraitLock";

    private MaterialSwitch portraitLockSwitch;
    private SharedPreferences sharedPreferences;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Find the switch from the layout
        portraitLockSwitch = view.findViewById(R.id.portrait_lock_switch);

        // Load the saved state and setup the switch listener
        loadAndSetPortraitLockState();
    }

    /**
     * Loads the saved portrait lock preference, sets the switch state,
     * and attaches a listener to handle user toggles.
     */
    private void loadAndSetPortraitLockState() {
        // 1. Load the saved preference (default to false if not found)
        boolean isLocked = sharedPreferences.getBoolean(PORTRAIT_LOCK_KEY, false);

        // 2. Set the switch's initial state without triggering the listener
        portraitLockSwitch.setChecked(isLocked);

        // 3. Set a listener that will only trigger on user interaction
        portraitLockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the new state
            savePortraitLockPreference(isChecked);
            // Apply the new orientation setting
            applyOrientationLock(isChecked);
        });
    }

    /**
     * Saves the user's portrait lock preference.
     * @param isLocked True if the app should be locked to portrait, false otherwise.
     */
    private void savePortraitLockPreference(boolean isLocked) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PORTRAIT_LOCK_KEY, isLocked);
        editor.apply(); // Apply changes asynchronously
    }

    /**
     * Applies the orientation lock to the current Activity.
     * @param isLocked True to lock to portrait, false to allow sensor-based rotation.
     */
    private void applyOrientationLock(boolean isLocked) {
        if (getActivity() != null) {
            if (isLocked) {
                // Lock the orientation to portrait
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                Toast.makeText(getContext(), "Orientation locked to Portrait", Toast.LENGTH_SHORT).show();
            } else {
                // Unlock the orientation, allowing the sensor to determine it
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Toast.makeText(getContext(), "Orientation unlocked", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
