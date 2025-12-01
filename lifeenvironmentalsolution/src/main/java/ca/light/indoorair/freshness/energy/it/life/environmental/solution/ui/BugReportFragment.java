package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class BugReportFragment extends Fragment {

    private Uri screenshotUri = null;
    private TextView screenshotStatusText;

    // Image picker launcher (lets the user choose an image from gallery / files)
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    screenshotUri = uri;
                    if (screenshotStatusText != null) {
                        screenshotStatusText.setText(
                                getString(R.string.bug_screenshot_selected)
                        );
                    }
                }
            });

    public BugReportFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bug_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editSummary = view.findViewById(R.id.edit_bug_summary);
        EditText editSteps = view.findViewById(R.id.edit_bug_steps);
        EditText editExpected = view.findViewById(R.id.edit_bug_expected);
        EditText editActual = view.findViewById(R.id.edit_bug_actual);
        EditText editExtra = view.findViewById(R.id.edit_bug_extra);
        Button submitButton = view.findViewById(R.id.button_submit_bug);
        Button pickScreenshotButton = view.findViewById(R.id.button_pick_screenshot);
        screenshotStatusText = view.findViewById(R.id.text_screenshot_status);

        // Initial status text
        if (screenshotStatusText != null) {
            screenshotStatusText.setText(getString(R.string.bug_screenshot_none));
        }

        // 🔹 Pick screenshot button
        if (pickScreenshotButton != null) {
            pickScreenshotButton.setOnClickListener(v ->
                    pickImageLauncher.launch("image/*")
            );
        }

        // 🔹 Submit bug report
        submitButton.setOnClickListener(v -> {
            String summary = editSummary.getText().toString().trim();
            String steps = editSteps.getText().toString().trim();
            String expected = editExpected.getText().toString().trim();
            String actual = editActual.getText().toString().trim();
            String extra = editExtra.getText().toString().trim();

            if (TextUtils.isEmpty(summary) || TextUtils.isEmpty(actual)) {
                Toast.makeText(requireContext(),
                        getString(R.string.bug_error_empty),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Build email body
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("Summary:\n").append(summary).append("\n\n");

            if (!TextUtils.isEmpty(steps)) {
                bodyBuilder.append("Steps to reproduce:\n")
                        .append(steps).append("\n\n");
            }
            if (!TextUtils.isEmpty(expected)) {
                bodyBuilder.append("Expected behavior:\n")
                        .append(expected).append("\n\n");
            }
            bodyBuilder.append("Actual behavior:\n")
                    .append(actual).append("\n\n");

            if (!TextUtils.isEmpty(extra)) {
                bodyBuilder.append("Additional notes:\n")
                        .append(extra).append("\n\n");
            }

            // Technical info
            bodyBuilder.append("--- Technical Info ---\n");
            try {
                PackageManager pm = requireContext().getPackageManager();
                PackageInfo pInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);
                String versionName = pInfo.versionName;
                int versionCode = (int) pInfo.getLongVersionCode();
                bodyBuilder.append("App Version: ")
                        .append(versionName).append(" (").append(versionCode).append(")\n");
            } catch (Exception e) {
                bodyBuilder.append("App Version: unknown\n");
            }

            bodyBuilder.append("Device: ")
                    .append(Build.MANUFACTURER).append(" ")
                    .append(Build.MODEL).append("\n");
            bodyBuilder.append("Android: ")
                    .append(Build.VERSION.RELEASE)
                    .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");

            String body = bodyBuilder.toString();

            String supportEmail = getString(R.string.help_support_email);
            String subject = getString(R.string.help_bug_email_subject);

            // Use ACTION_SEND so we can attach the screenshot
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("message/rfc822"); // bias towards email apps
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);

            // Attach screenshot if one was selected
            if (screenshotUri != null) {
                intent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            try {
                Toast.makeText(requireContext(),
                        getString(R.string.bug_submitted_to_email),
                        Toast.LENGTH_SHORT).show();
                startActivity(Intent.createChooser(intent,
                        getString(R.string.bug_submit)));
            } catch (Exception e) {
                Toast.makeText(requireContext(),
                        getString(R.string.help_no_email_app),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
