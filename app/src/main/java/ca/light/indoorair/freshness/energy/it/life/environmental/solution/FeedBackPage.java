package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class FeedBackPage extends AppCompatActivity {

    private static final String TAG = "FeedBackPage"; // Tag for logging

    private EditText etName, etEmail, etPhone, etDeviceModel, etFeedback;
    private RatingBar ratingBar;
    private Button btnSend;
    private DatabaseReference feedbackDbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback);

        feedbackDbRef = FirebaseDatabase.getInstance().getReference().child("Feedback");

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDeviceModel = findViewById(R.id.etDeviceModel);
        etFeedback = findViewById(R.id.etFeedback);
        ratingBar = findViewById(R.id.ratingBar);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String deviceModel = etDeviceModel.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();
        float rating = ratingBar.getRating();

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
            TextUtils.isEmpty(deviceModel) || TextUtils.isEmpty(feedback) || rating == 0) {
            Toast.makeText(this, "Please fill all fields and provide a rating.", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(FeedBackPage.this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                            etName.setText("");
                            etEmail.setText("");
                            etPhone.setText("");
                            etDeviceModel.setText("");
                            etFeedback.setText("");
                            ratingBar.setRating(0);
                        } else {
                            Log.e(TAG, "Firebase submission failed.", task.getException());
                            Toast.makeText(FeedBackPage.this, "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
