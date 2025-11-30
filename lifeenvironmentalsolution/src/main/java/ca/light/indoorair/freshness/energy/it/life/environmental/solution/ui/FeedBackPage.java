package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation.InputValidation;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel.FeedbackViewModel;

public class FeedBackPage extends Fragment {

    private EditText etName, etEmail, etPhone, etFeedback;
    private TextView tvTimer;
    private RatingBar ratingBar;
    private Button btnSend;
    private ProgressBar feedbackSubmissionProgressBar;
    private FeedbackViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return inflater.inflate(R.layout.feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);

        bindViews(view);
        observeViewModel();

        viewModel.checkSubmissionCooldown();

        btnSend.setOnClickListener(v -> submitFeedback());
    }

    private void bindViews(View view) {
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etFeedback = view.findViewById(R.id.etFeedback);
        ratingBar = view.findViewById(R.id.ratingBar);
        btnSend = view.findViewById(R.id.btnSend);
        tvTimer = view.findViewById(R.id.tvTimer);
        feedbackSubmissionProgressBar = view.findViewById(R.id.feedbackSubmissionProgressBar);
    }

    private void observeViewModel() {
        viewModel.isSubmitting.observe(getViewLifecycleOwner(), isSubmitting -> {
            feedbackSubmissionProgressBar.setVisibility(isSubmitting ? View.VISIBLE : View.GONE);
            btnSend.setEnabled(!isSubmitting);
        });

        viewModel.submissionSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Feedback Submitted")
                        .setIcon(R.drawable.logolife)
                        .setMessage("Thank you for your feedback!")
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> clearForm())
                        .show();
            }
        });

        viewModel.submissionError.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.timerText.observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                tvTimer.setText(text);
                tvTimer.setVisibility(View.VISIBLE);
            } else {
                tvTimer.setVisibility(View.GONE);
            }
        });

        viewModel.isSendButtonEnabled.observe(getViewLifecycleOwner(), isEnabled -> btnSend.setEnabled(isEnabled));
    }

    private void clearForm() {
        etName.setText("");
        etEmail.setText("");
        etPhone.setText("");
        etFeedback.setText("");
        ratingBar.setRating(0);
    }

    private void submitFeedback() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (!validateInputs(name, email, phone, feedback, rating)) {
            return;
        }

        viewModel.submitFeedback(name, email, phone, feedback, rating);
    }

    private boolean validateInputs(String name, String email, String phone, String feedback, float rating) {
        if (!InputValidation.isValidName(name)) {
            etName.setError("Please enter your name.");
            etName.requestFocus();
            return false;
        }

        if (!InputValidation.isValidEmail(email)) {
            etEmail.setError("Please enter a valid email address.");
            etEmail.requestFocus();
            return false;
        }

        if (!InputValidation.isValidPhone(phone)) {
            etPhone.setError("Please enter a valid phone number.");
            etPhone.requestFocus();
            return false;
        }

        if (!InputValidation.isValidFeedback(feedback)) {
            etFeedback.setError("Please provide your feedback.");
            etFeedback.requestFocus();
            return false;
        }

        if (rating == 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Incomplete Form")
                    .setMessage("Please provide a rating.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return false;
        }

        return true;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE);
        boolean isPortraitLock = sharedPreferences.getBoolean("portrait_lock", false);
        if (!isPortraitLock) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
}
