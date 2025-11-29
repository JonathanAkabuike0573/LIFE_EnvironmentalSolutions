package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AirQualityRepository {

    private final DatabaseReference databaseReference;

    public AirQualityRepository() {
        databaseReference = FirebaseDatabase.getInstance().getReference("sgp30_readings");
    }

    public void listenForSensorData(@NonNull ValueEventListener listener) {
        // Query for the single most recent reading
        databaseReference.limitToLast(1).addValueEventListener(listener);
    }

    public void removeListener(@NonNull ValueEventListener listener) {
        databaseReference.removeEventListener(listener);
    }

    public Task<DataSnapshot> manualRefresh() {
        // Force a one-time fresh read from the server for the latest reading
        return databaseReference.limitToLast(1).get();
    }
}
