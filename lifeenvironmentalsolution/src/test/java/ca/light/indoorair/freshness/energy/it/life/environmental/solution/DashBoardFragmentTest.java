package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

// A testable version of the fragment that allows us to check the greeting
class TestableDashBoardFragment extends DashBoardFragment {
    private String lastGreetingName;

    @Override
    protected void setGreeting(String name) {
        this.lastGreetingName = name;
    }

    public String getLastGreetingName() {
        return lastGreetingName;
    }
}

/**
 * Unit tests for the greeting logic in DashBoardFragment.
 */
public class DashBoardFragmentTest {

    @Test
    public void testGenerateGreetingMessage() {
        DashBoardFragment fragment = new DashBoardFragment();
        Calendar calendar = Calendar.getInstance();

        // Test morning greeting
        calendar.set(Calendar.HOUR_OF_DAY, 8); // 8 AM
        String morningGreeting = fragment.generateGreetingMessage("World", calendar);
        assertEquals("Good morning, World", morningGreeting);

        // Test afternoon greeting
        calendar.set(Calendar.HOUR_OF_DAY, 14); // 2 PM
        String afternoonGreeting = fragment.generateGreetingMessage("World", calendar);
        assertEquals("Good afternoon, World", afternoonGreeting);

        // Test evening greeting
        calendar.set(Calendar.HOUR_OF_DAY, 20); // 8 PM
        String eveningGreeting = fragment.generateGreetingMessage("World", calendar);
        assertEquals("Good evening, World", eveningGreeting);
    }

    @Test
    public void testLoadGreeting_Success() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        FakeUserDataProvider provider = new FakeUserDataProvider(true, "Kieran Sharma");
        fragment.loadGreeting(provider);

        assertEquals("Kieran", fragment.getLastGreetingName());
    }

    @Test
    public void testLoadGreeting_Failure() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        FakeUserDataProvider provider = new FakeUserDataProvider(false, null);
        fragment.loadGreeting(provider);

        assertEquals("User", fragment.getLastGreetingName());
    }

    @Test
    public void testGreetingBoundaries() {
        DashBoardFragment fragment = new DashBoardFragment();
        Calendar calendar = Calendar.getInstance();

        // Test just before noon
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good morning"));

        // Test exactly at noon
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good afternoon"));

        // Test just before evening
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good afternoon"));

        // Test exactly at evening
        calendar.set(Calendar.HOUR_OF_DAY, 18);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good evening"));
    }

    @Test
    public void testLoadGreeting_EmptyName() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        FakeUserDataProvider provider = new FakeUserDataProvider(true, "");
        fragment.loadGreeting(provider);

        assertEquals("User", fragment.getLastGreetingName());
    }

    @Test
    public void testLoadGreeting_WhitespaceName() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        FakeUserDataProvider provider = new FakeUserDataProvider(true, "   ");
        fragment.loadGreeting(provider);

        assertEquals("User", fragment.getLastGreetingName());
    }

    @Test
    public void testGreetingMessageIsNotNull() {
        DashBoardFragment fragment = new DashBoardFragment();
        Calendar calendar = Calendar.getInstance();
        String greeting = fragment.generateGreetingMessage("Test", calendar);
        assertNotNull("The greeting message should not be null", greeting);
    }

    @Test
    public void testGreetingMessageIsNotEmpty() {
        DashBoardFragment fragment = new DashBoardFragment();
        Calendar calendar = Calendar.getInstance();
        String greeting = fragment.generateGreetingMessage("Test", calendar);
        assertFalse("The greeting message should not be empty", greeting.isEmpty());
    }

    @Test
    public void testProviderIsNotNull() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        FakeUserDataProvider provider = new FakeUserDataProvider(true, "Test");
        fragment.setDataProvider(provider);
        assertNotNull("The data provider should not be null", fragment.dataProvider);
    }

    @Test
    public void testCalendarInstanceIsNotNull() {
        DashBoardFragment fragment = new DashBoardFragment();
        assertNotNull("The calendar instance should not be null", fragment.getCalendarInstance());
    } }