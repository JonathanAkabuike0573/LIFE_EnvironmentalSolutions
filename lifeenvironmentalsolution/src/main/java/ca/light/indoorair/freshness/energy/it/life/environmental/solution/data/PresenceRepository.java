package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PresenceRepository {

    private final DatabaseReference presenceRef;

    public PresenceRepository() {
        presenceRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
    }

    public void startListening(@NonNull ValueEventListener listener) {
        presenceRef.limitToLast(1).addValueEventListener(listener);
    }

    public void stopListening(@NonNull ValueEventListener listener) {
        presenceRef.removeEventListener(listener);
    }

    public Task<Void> manualOverride(String status) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
        Map<String, Object> overrideData = new HashMap<>();
        overrideData.put("room_status", status);
        overrideData.put("timestamp", timestamp);
        String key = presenceRef.push().getKey();
        if (key != null) {
            return presenceRef.child(key).setValue(overrideData);
        }
        return null;
    }
}
