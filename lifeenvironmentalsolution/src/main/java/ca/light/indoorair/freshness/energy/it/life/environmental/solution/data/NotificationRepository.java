package ca.light.indoorair.freshness.energy.it.life.environmental.solution.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.NotificationItem;

public class NotificationRepository {

    private final DatabaseReference notificationDbRef;
    private final MutableLiveData<List<NotificationItem>> _notifications = new MutableLiveData<>();
    private static NotificationRepository instance;


    private NotificationRepository() {
        notificationDbRef = FirebaseDatabase.getInstance().getReference("notifications");
        startListening();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    public LiveData<List<NotificationItem>> getNotifications() {
        return _notifications;
    }

    private void startListening() {

        notificationDbRef.limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<NotificationItem> items = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    NotificationItem item = child.getValue(NotificationItem.class);
                    if (item != null) {
                        item.setId(child.getKey());
                        items.add(item);
                    }
                }

                Collections.reverse(items);
                _notifications.setValue(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    public void addNotification(String message) {
        String key = notificationDbRef.push().getKey();
        if (key != null) {
            NotificationItem item = new NotificationItem(message, System.currentTimeMillis());
            notificationDbRef.child(key).setValue(item);
        }
    }

    public void deleteNotification(String id) {
        if (id != null) {
            notificationDbRef.child(id).removeValue();
        }
    }

    public void clearAll() {
        notificationDbRef.removeValue();
    }
}
