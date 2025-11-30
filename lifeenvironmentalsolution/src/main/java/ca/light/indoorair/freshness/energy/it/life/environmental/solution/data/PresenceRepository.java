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
    private String currentRoom;

    public PresenceRepository() {
        presenceRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
    }

    public void startListening(@NonNull ValueEventListener listener, String roomName) {
        currentRoom = roomName;  // Store for manualOverride

        DatabaseReference roomRef;
        if ("Main Office".equals(roomName)) {
            roomRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
        } else {
            roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("room_occupancy");
        }
        roomRef.limitToLast(1).addValueEventListener(listener);
    }

    public void stopListening(@NonNull ValueEventListener listener, String roomName) {
        // Remove listener from correct path
        DatabaseReference roomRef;
        if ("Main Office".equals(roomName)) {
            roomRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
        } else {
            roomRef = FirebaseDatabase.getInstance().getReference("rooms").child(roomName).child("room_occupancy");
        }
        roomRef.removeEventListener(listener);
    }

    public Task<Void> manualOverride(String status) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> firebaseData = new HashMap<>();
        firebaseData.put("ina219_readings", 0.0);
        firebaseData.put("room_status", status.toLowerCase());
        firebaseData.put("comfort_level", "neutral");
        firebaseData.put("current_temperature_c", 22.5);
        firebaseData.put("current_temperature_f", 72.5);
        firebaseData.put("derivative_level", "low");
        firebaseData.put("location", currentRoom != null ? currentRoom : "Main Office");
        firebaseData.put("motion_level", status.toLowerCase().equals("occupied") ? "high" : "low");
        firebaseData.put("sensor_type", "AK9753");
        firebaseData.put("session_duration_seconds", 0);
        firebaseData.put("temperature_source", "sense_hat");
        firebaseData.put("timestamp", timestamp);
        firebaseData.put("manual_override", true);

        String key = presenceRef.push().getKey();
        if (key != null) {
            return presenceRef.child(key).setValue(firebaseData);
        }
        return null;
    }

}
