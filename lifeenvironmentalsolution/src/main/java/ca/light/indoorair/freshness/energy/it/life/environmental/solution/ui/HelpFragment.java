package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;



import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class HelpFragment extends Fragment {

    public HelpFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button quickStartButton = view.findViewById(R.id.button_view_guide);
        Button contactSupportButton = view.findViewById(R.id.button_contact_support);
        Button reportBugButton = view.findViewById(R.id.button_report_bug);

        // Quick Start
        if (quickStartButton != null) {
            quickStartButton.setOnClickListener(v ->
                    Toast.makeText(requireContext(),
                            "Quick start guide coming soon!",
                            Toast.LENGTH_SHORT).show()
            );
        }

        // Contact Support
        if (contactSupportButton != null) {
            contactSupportButton.setOnClickListener(v -> {
                String[] emails = new String[]{
                        getString(R.string.help_support_email)
                };
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, emails);
                intent.putExtra(Intent.EXTRA_SUBJECT,
                        getString(R.string.help_support_email_subject));

                try {
                    startActivity(Intent.createChooser(intent,
                            getString(R.string.help_contact_support)));
                } catch (Exception e) {
                    Toast.makeText(requireContext(),
                            getString(R.string.help_no_email_app),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Report Bug
        if (reportBugButton != null) {
            reportBugButton.setOnClickListener(v -> {
                String supportEmail = getString(R.string.help_support_email);
                String subject = getString(R.string.help_bug_email_subject);

                StringBuilder bodyBuilder = new StringBuilder();
                bodyBuilder.append(getString(R.string.help_bug_email_body));
                bodyBuilder.append("\n");

                try {
                    PackageManager pm = requireContext().getPackageManager();
                    PackageInfo pInfo = pm.getPackageInfo(requireContext().getPackageName(), 0);
                    String versionName = pInfo.versionName;
                    int versionCode = (int) pInfo.getLongVersionCode();
                    bodyBuilder.append("App Version: ").append(versionName)
                            .append(" (").append(versionCode).append(")\n");
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

                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{supportEmail});
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                intent.putExtra(Intent.EXTRA_TEXT, body);

                try {
                    startActivity(Intent.createChooser(intent,
                            getString(R.string.help_report_bug)));
                } catch (Exception e) {
                    Toast.makeText(requireContext(),
                            getString(R.string.help_no_email_app),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
