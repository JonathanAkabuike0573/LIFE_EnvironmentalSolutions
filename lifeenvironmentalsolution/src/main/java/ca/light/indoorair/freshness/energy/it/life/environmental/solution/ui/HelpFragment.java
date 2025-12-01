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

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.MainActivity;
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

        // Contact Support (opens email directly)
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

        // Report Bug – open full BugReportFragment page (feedback form)
        if (reportBugButton != null) {
            reportBugButton.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity())
                            .setCurrentFragment(new BugReportFragment(), true);
                } else {
                    Toast.makeText(requireContext(),
                            "Unable to open bug report page.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
