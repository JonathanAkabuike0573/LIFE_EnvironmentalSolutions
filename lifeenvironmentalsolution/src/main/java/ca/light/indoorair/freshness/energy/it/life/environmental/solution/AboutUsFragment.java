package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Note: Ensure your build.gradle file includes androidx.appcompat or
// androidx.fragment dependencies.

/**
 * A simple {@link Fragment} subclass for displaying the About Us screen.
 * This fragment inflates the layout and sets the dynamic app version text.
 */
public class AboutUsFragment extends Fragment {

    // Default public constructor is required for Fragments
    public AboutUsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the TextView where the app version will be displayed
        TextView appVersionText = view.findViewById(R.id.app_version_text);

        String versionName = "N/A";
        int versionCode = 0;

        try {
            // Get the application's package information
            versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;

            // Note: For API 28 and below, getLongVersionCode() is not available.
            // Using getVersionCode() for broader compatibility.
            versionCode = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionCode;

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Construct the full version string, e.g., "Version 1.0.0 (1)"
        String fullVersionInfo = getString(R.string.version_label, versionName, versionCode);
        appVersionText.setText(fullVersionInfo);
    }
}