package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import java.util.Date;

public class NotificationItem {
    private final String text;
    private final Date date;

    public NotificationItem(String text, Date date) {
        this.text = text;
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public Date getDate() {
        return date;
    }
}
