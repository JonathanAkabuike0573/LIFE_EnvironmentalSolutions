package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Date;

@IgnoreExtraProperties
public class NotificationItem {
    private String id;
    private String text;
    private long timestamp;

    // Empty constructor required for Firebase
    public NotificationItem() {}


    public NotificationItem(String text, long timestamp) {
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Helper method for your Adapter to get a Date object
    public Date getDate() {
        return new Date(timestamp);
    }
}
