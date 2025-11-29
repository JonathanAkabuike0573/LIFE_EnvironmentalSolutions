package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class FeedbackRepository {

    private final DatabaseReference feedbackDbRef;

    public FeedbackRepository() {
        feedbackDbRef = FirebaseDatabase.getInstance().getReference().child("Feedback");
    }

    public Task<Void> submitFeedback(HashMap<String, Object> feedbackMap) {
        String feedbackId = feedbackDbRef.push().getKey();
        if (feedbackId != null) {
            return feedbackDbRef.child(feedbackId).setValue(feedbackMap);
        } else {
            return Tasks.forException(new Exception("Failed to create a new feedback entry."));
        }
    }
}
