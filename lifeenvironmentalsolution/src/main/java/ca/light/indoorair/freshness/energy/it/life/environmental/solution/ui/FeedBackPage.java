package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation.InputValidation;

public class FeedBackPage extends Fragment {

    private static final String TAG = "FeedBackPage"; // Tag for logging

    private EditText etName, etEmail, etPhone, etFeedback;
    private TextView tvTimer;
    private static final String PREFS_NAME = "FeedbackPrefs";
    private static final String LAST_SUBMISSION_TIMESTAMP = "last_submission_timestamp";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000;
    private RatingBar ratingBar;
    private Button btnSend;
    private DatabaseReference feedbackDbRef;
    private ProgressBar feedbackSubmissionProgressBar;
    private CountDownTimer countDownTimer;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return inflater.inflate(R.layout.feedback, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        feedbackDbRef = FirebaseDatabase.getInstance().getReference().child("Feedback");

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etFeedback = view.findViewById(R.id.etFeedback);
        ratingBar = view.findViewById(R.id.ratingBar);
        btnSend = view.findViewById(R.id.btnSend);
        tvTimer = view.findViewById(R.id.tvTimer);
        feedbackSubmissionProgressBar = view.findViewById(R.id.feedbackSubmissionProgressBar);

        checkSubmissionCooldown();

        btnSend.setOnClickListener(v -> submitFeedback());
    }

    private void checkSubmissionCooldown() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSubmissionTime = prefs.getLong(LAST_SUBMISSION_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastSubmissionTime;

        if (timeDifference < TWENTY_FOUR_HOURS_MILLIS) {
            btnSend.setEnabled(false);
            long remainingTime = TWENTY_FOUR_HOURS_MILLIS - timeDifference;
            startTimer(remainingTime);
        } else {
            btnSend.setEnabled(true);
            tvTimer.setVisibility(View.GONE);
        }
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        btnSend.setEnabled(false);
        tvTimer.setVisibility(View.VISIBLE);

        countDownTimer = new CountDownTimer(duration, 1000) { // Update every second
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "You can submit again in: %02d:%02d:%02d", hours, minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                btnSend.setEnabled(true);
            }
        }.start();
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


    private void submitFeedback() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (!validateInputs(name, email, phone, feedback, rating)) {
            return;
        }

        // Programmatically get the device model
        String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;

        // --- Start of new logging ---
        Log.d(TAG, "Submit button clicked. Reading data...");
        Log.d(TAG, "Name: " + name);
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "Phone: " + phone);
        Log.d(TAG, "Device Model: " + deviceModel);
        Log.d(TAG, "Feedback: " + feedback);
        Log.d(TAG, "Rating: " + rating);

        String feedbackId = feedbackDbRef.push().getKey();

        HashMap<String, Object> feedbackMap = new HashMap<>();
        feedbackMap.put("name", name);
        feedbackMap.put("email", email);
        feedbackMap.put("phone", phone);
        feedbackMap.put("deviceModel", deviceModel);
        feedbackMap.put("rating", rating);
        feedbackMap.put("message", feedback);

        // Show progress bar and disable button
        feedbackSubmissionProgressBar.setVisibility(View.VISIBLE);
        btnSend.setEnabled(false);

        if (feedbackId != null) {
            Log.d(TAG, "Preparing to send data to Firebase with ID: " + feedbackId);

            // Simulate network delay for 5 seconds before attempting to write to Firebase
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                feedbackDbRef.child(feedbackId).setValue(feedbackMap)
                        .addOnCompleteListener(task -> {
                            // Hide progress bar
                            feedbackSubmissionProgressBar.setVisibility(View.GONE);

                            if (task.isSuccessful()) {
                                Log.d(TAG, "Firebase submission successful!");

                                // Show success dialog
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Feedback Submitted")
                                        .setIcon(R.drawable.logolife)
                                        .setMessage("Thank you for your feedback!")
                                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                            // Store the submission timestamp
                                            SharedPreferences.Editor editor = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                                            editor.putLong(LAST_SUBMISSION_TIMESTAMP, System.currentTimeMillis());
                                            editor.apply();
                                            Log.d(TAG, "Stored current time as last submission timestamp.");

                                            // Clear the form fields after user clicks OK
                                            etName.setText("");
                                            etEmail.setText("");
                                            etPhone.setText("");
                                            etFeedback.setText("");
                                            ratingBar.setRating(0);
                                            startTimer(TWENTY_FOUR_HOURS_MILLIS);
                                        })
                                        .show();
                            } else {
                                Log.e(TAG, "Firebase submission failed.", task.getException());
                                btnSend.setEnabled(true);
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Submission Failed")
                                        .setMessage("Failed to submit feedback. Please check your network connection and try again.")
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show();
                            }
                        });
            }, 5000); // 5-second delay
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isPortraitLock = sharedPreferences.getBoolean("portrait_lock", false);
        if (!isPortraitLock) {
            requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }
}
