package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.model.Notification;

public class NotificationRepository {

    private final DatabaseReference notificationDbRef;

    public NotificationRepository() {
        notificationDbRef = FirebaseDatabase.getInstance().getReference().child("notifications");
    }

    public Task<Void> saveNotification(Notification notification) {
        String key = notificationDbRef.push().getKey();
        if (key != null) {
            return notificationDbRef.child(key).setValue(notification);
        }
        return null;
    }
}
