// C:/Users/jonat/AndroidStudioProjects/LIFE_EnvironmentalSolution/lifeenvironmentalsolution/src/main/java/ca/light/indoorair/freshness/energy/it/life/environmental/solution/SettingsFragment.java

//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

// Removed unused imports for profile management
// import android.widget.RelativeLayout;
// import androidx.appcompat.app.AlertDialog;
// import androidx.appcompat.app.AppCompatActivity;
// import androidx.appcompat.widget.Toolbar;
// import androidx.fragment.app.FragmentManager;
// import androidx.fragment.app.FragmentTransaction;
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;
// import android.widget.EditText;


public class SettingsFragment extends Fragment {

    // Define SharedPreferences constants
    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";
    private static final String PORTRAIT_LOCK_KEY = "portrait_lock";
    private static final String SMART_NOTIFICATION_KEY = "smart_notification";
    private static final String MORNING_REPORT_KEY = "morning_report";
    private static final String EVENING_REPORT_KEY = "evening_report";

    // REMOVED: private RelativeLayout profileManagement;
    // REMOVED: private RelativeLayout changePasswordLayout;

    private SharedPreferences sharedPreferences;
    // REMOVED: private FirebaseAuth mAuth; // This can be removed if not used elsewhere in this fragment

    private MaterialSwitch switchRequestingPermission;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            if (switchRequestingPermission != null) {
                switchRequestingPermission.setChecked(true);
                String key = (String) switchRequestingPermission.getTag();
                if (key != null) {
                    sharedPreferences.edit().putBoolean(key, true).apply();
                }
            }
        } else {
            Toast.makeText(getContext(), "Notification permission denied", Toast.LENGTH_SHORT).show();
        }
        switchRequestingPermission = null;
    });

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // REMOVED: All code related to profileManagement and changePasswordLayout
        // The onClickListeners have been removed.

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupThemeSwitch(view);
        setupPortraitSwitch(view);
        setupNotificationSwitch(view.findViewById(R.id.sw_smart_notification), SMART_NOTIFICATION_KEY, "Smart notifications enabled", "Smart notifications disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_morning_report), MORNING_REPORT_KEY, "Morning report enabled", "Morning report disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_evening_reports), EVENING_REPORT_KEY, "Evening report enabled", "Evening report disabled");
    }

    // setupThemeSwitch, setupPortraitSwitch, and setupNotificationSwitch methods remain unchanged.
    // ...

    // REMOVED: showChangePasswordDialog() and changePassword() methods.
    // These should be moved to your AccountFragment.

    private void setupThemeSwitch(View view) {
        MaterialSwitch themeSwitch = view.findViewById(R.id.sw_dark_mode);
        themeSwitch.setChecked(sharedPreferences.getBoolean(THEME_KEY, false));

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(THEME_KEY, isChecked).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }

    private void setupPortraitSwitch(View view) {
        MaterialSwitch portraitSwitch = view.findViewById(R.id.sw_portrait);
        portraitSwitch.setChecked(sharedPreferences.getBoolean(PORTRAIT_LOCK_KEY, false));

        portraitSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PORTRAIT_LOCK_KEY, isChecked).apply();
            Activity activity = getActivity();
            if (activity != null) {
                activity.setRequestedOrientation(isChecked ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        });
    }

    private void setupNotificationSwitch(MaterialSwitch switchView, String key, String enabledMessage, String disabledMessage) {
        switchView.setTag(key);
        switchView.setChecked(sharedPreferences.getBoolean(key, false));

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    switchRequestingPermission = switchView;
                    switchView.setChecked(false);
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    sharedPreferences.edit().putBoolean(key, true).apply();
                    Toast.makeText(getContext(), enabledMessage, Toast.LENGTH_SHORT).show();
                }
            } else {
                sharedPreferences.edit().putBoolean(key, false).apply();
                Toast.makeText(getContext(), disabledMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
