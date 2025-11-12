package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
    private RatingBar ratingBar;
    private Button btnSend;
    private DatabaseReference feedbackDbRef;

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

        btnSend.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
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
        // --- End of new logging ---

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(feedback) || rating == 0) {
            Toast.makeText(requireContext(), "Please fill all fields and provide a rating.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Submission failed: Not all fields were filled."); // Log a warning
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

        if (feedbackId != null) {
            Log.d(TAG, "Preparing to send data to Firebase with ID: " + feedbackId);
            feedbackDbRef.child(feedbackId).setValue(feedbackMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firebase submission successful!");
                            Toast.makeText(requireContext(), "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                            etName.setText("");
                            etEmail.setText("");
                            etPhone.setText("");
                            etFeedback.setText("");
                            ratingBar.setRating(0);
                        } else {
                            Log.e(TAG, "Firebase submission failed.", task.getException());
                            Toast.makeText(requireContext(), "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
