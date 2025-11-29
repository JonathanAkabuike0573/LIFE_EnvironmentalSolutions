package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<NotificationItem> notifications;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClicked(NotificationItem item);
    }

    public NotificationAdapter(List<NotificationItem> notifications, OnItemLongClickListener longClickListener) {
        this.notifications = notifications;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notificationItem = notifications.get(position);
        holder.notificationTextView.setText(notificationItem.getText());
        holder.notificationDateView.setText(getFormattedDate(notificationItem.getDate()));
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClicked(notificationItem);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<NotificationItem> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    private String getFormattedDate(java.util.Date date) {
        if (DateUtils.isToday(date.getTime())) {
            return "Today";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return sdf.format(date);
        }
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTextView;
        TextView notificationDateView;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTextView = itemView.findViewById(R.id.notification_text);
            notificationDateView = itemView.findViewById(R.id.notification_date);
        }
    }
}
