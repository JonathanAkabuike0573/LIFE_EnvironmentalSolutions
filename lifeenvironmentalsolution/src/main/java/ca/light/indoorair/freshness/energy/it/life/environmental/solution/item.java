package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

public class item {
    String name;
    String status;
    int images;
    boolean isDeviceOn;
    boolean showToggle;

    public item(String name, String status, int images, boolean isDeviceOn, boolean showToggle) {
        this.name = name;
        this.status = status;
        this.images = images;
        this.isDeviceOn = isDeviceOn;
        this.showToggle = showToggle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getImages() {
        return images;
    }

    public void setImages(int images) {
        this.images = images;
    }

    public boolean isDeviceOn() {
        return isDeviceOn;
    }

    public void setDeviceOn(boolean deviceOn) {
        isDeviceOn = deviceOn;
    }

    public boolean isShowToggle() {
        return showToggle;
    }

    public void setShowToggle(boolean showToggle) {
        this.showToggle = showToggle;
    }
}
