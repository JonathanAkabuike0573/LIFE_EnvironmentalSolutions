//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    // Define SharedPreferences constants
    public static final String PREFS_NAME = "MyPrefsFile";
    private static final String PORTRAIT_LOCK_KEY = "portrait_lock";
    private static final String SMART_NOTIFICATION_KEY = "smart_notification";
    private static final String MORNING_REPORT_KEY = "morning_report";
    private static final String EVENING_REPORT_KEY = "evening_report";

    private RelativeLayout profileManagement;
    private RelativeLayout changePasswordLayout;

    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    private MaterialSwitch switchRequestingPermission;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            if (switchRequestingPermission != null) {
                switchRequestingPermission.setChecked(true);
                // Also save the state now that permission is granted
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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        profileManagement = view.findViewById(R.id.row_account);
        changePasswordLayout = view.findViewById(R.id.row_change_password);
        mAuth = FirebaseAuth.getInstance();

        profileManagement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getActivity() != null) {
                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.profile_management);
                    Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
                    if (toolbar != null) {
                        toolbar.setTitle(R.string.profile_management);
                    }
                }

                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.main, new AccountFragment());
                fragmentTransaction.addToBackStack(null); // Optional: if you want to navigate back
                fragmentTransaction.commit();

            }
        });

        changePasswordLayout.setOnClickListener(v -> showChangePasswordDialog());

        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupPortraitSwitch(view);
        setupNotificationSwitch(view.findViewById(R.id.sw_smart_notification), SMART_NOTIFICATION_KEY, "Smart notifications enabled", "Smart notifications disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_morning_report), MORNING_REPORT_KEY, "Morning report enabled", "Morning report disabled");
        setupNotificationSwitch(view.findViewById(R.id.sw_evening_reports), EVENING_REPORT_KEY, "Evening report enabled", "Evening report disabled");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.settings);
            Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setTitle(R.string.settings);
            }
        }
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
        switchView.setTag(key); // Store the key for later use
        switchView.setChecked(sharedPreferences.getBoolean(key, false));

        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    switchRequestingPermission = switchView; // Use the switchView from the outer scope
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

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        View viewInflated = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, (ViewGroup) getView(), false);
        final EditText newPasswordEditText = viewInflated.findViewById(R.id.new_password_edit_text);
        final EditText confirmPasswordEditText = viewInflated.findViewById(R.id.confirm_password_edit_text);

        builder.setView(viewInflated);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String newPassword = newPasswordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (newPassword.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(newPassword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void changePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Password updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
