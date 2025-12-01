package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import java.util.List;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.NotificationRepository;

public class NotificationViewModel extends ViewModel {

    private final NotificationRepository repository;

    public NotificationViewModel() {
        repository = NotificationRepository.getInstance();
    }

    public LiveData<List<NotificationItem>> getNotifications() {
        return repository.getNotifications();
    }

    // The repository starts listening automatically, so no manual load needed

    public void clearAllNotifications() {
        repository.clearAll();
    }

    public void deleteNotification(NotificationItem item) {
        repository.deleteNotification(item.getId());
    }
}
