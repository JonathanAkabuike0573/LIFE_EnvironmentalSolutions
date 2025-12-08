//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;



public class SettingsFragment extends Fragment {


    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";
    private static final String PORTRAIT_LOCK_KEY = "portrait_lock";
    private static final String UNITS_KEY = "units";
    private static final String SMART_NOTIFICATION_KEY = "smart_notification";
    private static final String MORNING_REPORT_KEY = "morning_report";
    private static final String EVENING_REPORT_KEY = "evening_report";


    private SharedPreferences sharedPreferences;


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
            Toast.makeText(getContext(), R.string.notification_permission_denied, Toast.LENGTH_SHORT).show();
        }
        switchRequestingPermission = null;
    });

    public SettingsFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);



        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupThemeSwitch(view);
        setupPortraitSwitch(view);
        setupUnitsSpinner(view);
        setupNotificationSwitch(view.findViewById(R.id.sw_smart_notification), SMART_NOTIFICATION_KEY, "Smart notifications enabled", "Smart notifications disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_morning_report), MORNING_REPORT_KEY, "Morning report enabled", "Morning report disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_evening_reports), EVENING_REPORT_KEY, "Evening report enabled", "Evening report disabled");
    }



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

    private void setupUnitsSpinner(View view) {
        Spinner unitsSpinner = view.findViewById(R.id.spinner_units);


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.units_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitsSpinner.setAdapter(adapter);


        String currentUnit = sharedPreferences.getString(UNITS_KEY, "Metric (°C)");
        int spinnerPosition = adapter.getPosition(currentUnit);
        if (spinnerPosition >= 0) {
            unitsSpinner.setSelection(spinnerPosition);
        }


        unitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUnit = (String) parent.getItemAtPosition(position);
                sharedPreferences.edit().putString(UNITS_KEY, selectedUnit).apply();


                if (selectedUnit.equals("Metric (°C)")) {
                    Toast.makeText(getContext(), R.string.temperature_units_set_to_celsius , Toast.LENGTH_SHORT).show();
                } else if (selectedUnit.equals("Imperial (°F)")) {
                    Toast.makeText(getContext(), R.string.temperature_units_set_to_fahrenheit , Toast.LENGTH_SHORT).show();
                }


                notifyUnitChange(selectedUnit);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void notifyUnitChange(String newUnit) {

        sharedPreferences.edit().putString("last_unit_change", newUnit).apply();
        sharedPreferences.edit().putLong("unit_change_timestamp", System.currentTimeMillis()).apply();
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
