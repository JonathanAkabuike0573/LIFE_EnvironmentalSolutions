package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class FeedBackPage extends Fragment {

    private static final String TAG = "FeedBackPage"; // Tag for logging

    private EditText etName, etEmail, etPhone, etFeedback;
    private static final String PREFS_NAME = "FeedbackPrefs";
    private static final String LAST_SUBMISSION_TIMESTAMP = "last_submission_timestamp";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000;
    private RatingBar ratingBar;
    private Button btnSend;
    private DatabaseReference feedbackDbRef;
    private ProgressBar feedbackSubmissionProgressBar;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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
        feedbackSubmissionProgressBar = view.findViewById(R.id.feedbackSubmissionProgressBar);


        btnSend.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        // Check if the user has submitted feedback in the last 24 hours
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastSubmissionTime = prefs.getLong(LAST_SUBMISSION_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastSubmissionTime < TWENTY_FOUR_HOURS_MILLIS) {
            // It's been less than 24 hours
            new AlertDialog.Builder(requireContext())
                    .setTitle("Submission Limit")
                    .setIcon(R.drawable.logolife)
                    .setMessage("You can only submit feedback once every 24 hours.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            Log.w(TAG, "Submission blocked: User tried to submit feedback before 24-hour cooldown.");
            return;
        }

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();
        float rating = ratingBar.getRating();

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


        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(feedback) || rating == 0) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Incomplete Form")
                    .setMessage("Please fill all fields and provide a rating.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            Log.w(TAG, "Submission failed: Not all fields were filled.");
            return;
        }

        // Validate email and phone number
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            return;
        }
        if (!Patterns.PHONE.matcher(phone).matches()) {
            etPhone.setError("Please enter a valid phone number");
            return;
        }

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
                            // Hide progress bar and re-enable button regardless of outcome
                            feedbackSubmissionProgressBar.setVisibility(View.GONE);
                            btnSend.setEnabled(true);

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
                                        })
                                        .show();
                            } else {
                                Log.e(TAG, "Firebase submission failed.", task.getException());
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
}
