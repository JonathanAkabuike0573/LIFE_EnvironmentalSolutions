package ca.light.indoorair.freshness.energy.it.life.environmental.solution.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.MainActivity;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.NotificationRepository;

public class NotificationHelper {


    private static final String CHANNEL_ID = "smart_building_alerts";
    private static final String CHANNEL_NAME = "Smart Building Alerts";
    private static final int NOTIFICATION_ID = 1001;


    public static void sendAlert(Context context, String roomName, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts for occupancy, air quality, and energy");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );


        int iconRes = R.drawable.sensor_occupied;
        if (message.toLowerCase().contains("co2") || message.toLowerCase().contains("air")) {

        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle("Alert: " + roomName)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);


        int uniqueId = (int) System.currentTimeMillis();
        notificationManager.notify(uniqueId, builder.build());


        NotificationRepository.getInstance().addNotification(message);
    }
}
