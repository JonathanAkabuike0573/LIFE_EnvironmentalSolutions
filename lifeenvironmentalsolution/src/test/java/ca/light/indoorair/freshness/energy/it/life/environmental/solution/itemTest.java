package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import org.junit.Test;
import static org.junit.Assert.*;

public class itemTest {

    @Test
    public void testConstructorInitializesCorrectly() {
        item i = new item("Lamp", "Off", 123, false, true);
        assertEquals("Lamp", i.getName());
        assertEquals("Off", i.getStatus());
        assertEquals(123, i.getImages());
        assertFalse(i.isDeviceOn());
        assertTrue(i.isShowToggle());
    }

    @Test
    public void testSetName() {
        item i = new item("Lamp", "Off", 123, false, true);
        i.setName("Fan");
        assertEquals("Fan", i.getName());
    }

    @Test
    public void testSetStatus() {
        item i = new item("Lamp", "Off", 123, false, true);
        i.setStatus("On");
        assertEquals("On", i.getStatus());
    }

    @Test
    public void testSetImages() {
        item i = new item("Lamp", "Off", 123, false, true);
        i.setImages(456);
        assertEquals(456, i.getImages());
    }

    @Test
    public void testSetDeviceOn() {
        item i = new item("Lamp", "Off", 123, false, true);
        i.setDeviceOn(true);
        assertTrue(i.isDeviceOn());
    }

    @Test
    public void testSetShowToggle() {
        item i = new item("Lamp", "Off", 123, false, true);
        i.setShowToggle(false);
        assertFalse(i.isShowToggle());
    }

    @Test
    public void testDeviceInitiallyOn() {
        item i = new item("Lamp", "On", 123, true, true);
        assertTrue(i.isDeviceOn());
    }

    @Test
    public void testDeviceInitiallyOff() {
        item i = new item("Lamp", "Off", 123, false, true);
        assertFalse(i.isDeviceOn());
    }

    @Test
    public void testEmptyName() {
        item i = new item("", "Off", 123, false, true);
        assertEquals("", i.getName());
    }

    @Test
    public void testNullStatus() {
        item i = new item("Lamp", null, 123, false, true);
        assertNull(i.getStatus());
    }

    @Test
    public void testStatusNotNull() {
        item i = new item("Lamp", "Working", 123, true, true);
        assertNotNull(i.getStatus());
    }

    @Test
    public void testNotEqualsName() {
        item i = new item("Lamp", "Off", 123, false, true);
        assertNotEquals("Fan", i.getName());
    }
}
