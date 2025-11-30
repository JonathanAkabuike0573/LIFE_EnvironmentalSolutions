package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AirQualityRepository {

    private final DatabaseReference baseRef;

    public AirQualityRepository() {
        baseRef = FirebaseDatabase.getInstance().getReference();
    }

    private DatabaseReference getDatabaseReference(String roomName) {
        if ("Main Office".equals(roomName)) {
            return baseRef.child("sgp30_readings");
        } else {
            return baseRef.child("rooms").child(roomName).child("air_quality_readings");
        }
    }

    public void listenForSensorData(@NonNull ValueEventListener listener, String roomName) {
        getDatabaseReference(roomName).limitToLast(1).addValueEventListener(listener);
    }

    public void removeListener(@NonNull ValueEventListener listener, String roomName) {
        if (roomName != null) {
            getDatabaseReference(roomName).removeEventListener(listener);
        }
    }

    public Task<DataSnapshot> manualRefresh(String roomName) {
        return getDatabaseReference(roomName).limitToLast(1).get();
    }
}
