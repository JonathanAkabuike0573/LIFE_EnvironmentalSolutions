package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class NotificationViewModel extends ViewModel {

    private final MutableLiveData<List<NotificationItem>> notifications = new MutableLiveData<>();

    public LiveData<List<NotificationItem>> getNotifications() {
        return notifications;
    }

    public void loadNotifications() {
        ArrayList<NotificationItem> notificationList = new ArrayList<>();
        notificationList.add(new NotificationItem("High CO2 detected in the living room.", new Date()));
        notificationList.add(new NotificationItem("Window opened in the bedroom.", getYesterday()));
        notificationList.add(new NotificationItem("Energy consumption is higher than usual.", getYesterday()));
        notificationList.add(new NotificationItem("Air quality is poor in the kitchen.", new Date()));
        notificationList.add(new NotificationItem("New device detected on the network.", getPastDate()));
        notifications.setValue(notificationList);
    }

    public void clearAllNotifications() {
        notifications.setValue(new ArrayList<>());
    }

    public void deleteNotification(NotificationItem notificationItem) {
        List<NotificationItem> currentNotifications = notifications.getValue();
        if (currentNotifications != null) {
            currentNotifications.remove(notificationItem);
            notifications.setValue(new ArrayList<>(currentNotifications));
        }
    }

    private Date getYesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    private Date getPastDate() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2023, 10, 14); // November 14, 2023
        return cal.getTime();
    }
}
