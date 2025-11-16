//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsFragment extends Fragment {

    // Define SharedPreferences constants
    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String PORTRAIT_LOCK_KEY = "portrait_lock";
    private static final String SMART_NOTIFICATION_KEY = "smart_notification";
    private static final String MORNING_REPORT_KEY = "morning_report";
    private static final String EVENING_REPORT_KEY = "evening_report";

    private RelativeLayout profileManagement;

    private SharedPreferences sharedPreferences;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        profileManagement = view.findViewById(R.id.row_account);

        profileManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.main, new AccountFragment());
                fragmentTransaction.addToBackStack(null); // Optional: if you want to navigate back
                fragmentTransaction.commit();

            }
        });

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupPortraitSwitch(view);
        setupSmartNotificationSwitch(view);
        setupMorningReportSwitch(view);
        setupEveningReportSwitch(view);
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

    private void setupSmartNotificationSwitch(View view) {
        MaterialSwitch smartNotificationSwitch = view.findViewById(R.id.sw_smart_notification);
        smartNotificationSwitch.setChecked(sharedPreferences.getBoolean(SMART_NOTIFICATION_KEY, false));

        smartNotificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(SMART_NOTIFICATION_KEY, isChecked).apply();
            String message = isChecked ? "Smart notifications enabled" : "Smart notifications disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupMorningReportSwitch(View view) {
        MaterialSwitch morningReportSwitch = view.findViewById(R.id.sw_morning_report);
        morningReportSwitch.setChecked(sharedPreferences.getBoolean(MORNING_REPORT_KEY, false));

        morningReportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(MORNING_REPORT_KEY, isChecked).apply();
            String message = isChecked ? "Morning report enabled" : "Morning report disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupEveningReportSwitch(View view) {
        MaterialSwitch eveningReportSwitch = view.findViewById(R.id.sw_evening_reports);
        eveningReportSwitch.setChecked(sharedPreferences.getBoolean(EVENING_REPORT_KEY, false));

        eveningReportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(EVENING_REPORT_KEY, isChecked).apply();
            String message = isChecked ? "Evening report enabled" : "Evening report disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }
}
