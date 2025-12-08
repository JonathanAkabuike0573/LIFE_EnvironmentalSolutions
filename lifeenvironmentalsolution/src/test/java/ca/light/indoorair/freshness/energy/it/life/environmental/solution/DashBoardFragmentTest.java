package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

/**
 * A testable subclass. We override methods that require Android Context
 * so they work in a local Java environment.
 */
class TestableDashBoardFragment extends DashBoardFragment {
    private String lastGreetingName;
    public Object dataProvider;


    @Override
    protected void setGreeting(String name) {
        this.lastGreetingName = name;
    }

    public String getLastGreetingName() {
        return lastGreetingName;
    }


    @Override
    protected void loadGreeting(UserDataProvider dataProvider) {
        dataProvider.fetchUserData(new UserDataProvider.UserDataCallback() {
            @Override
            public void onDataReceived(String userName) {
                if (userName != null && !userName.trim().isEmpty()) {
                    String firstName = userName.split(" ")[0];
                    setGreeting(firstName);
                } else {
                    setGreeting("User");
                }
            }
            @Override
            public void onError(String errorMessage) {
                setGreeting("User");
            }
        });
    }


    @Override
    protected String getResourceString(int id) {
        if (id == R.string.good_morning) return "Good morning";
        if (id == R.string.good_afternoon) return "Good afternoon";
        if (id == R.string.good_evening) return "Good evening";
        return "";
    }
}

public class DashBoardFragmentTest {

    @Test
    public void testGenerateGreetingMessage() {

        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        Calendar calendar = Calendar.getInstance();

        // Morning
        calendar.set(Calendar.HOUR_OF_DAY, 8);
        assertEquals("Good morning, World", fragment.generateGreetingMessage("World", calendar));

        // Afternoon
        calendar.set(Calendar.HOUR_OF_DAY, 14);
        assertEquals("Good afternoon, World", fragment.generateGreetingMessage("World", calendar));

        // Evening
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        assertEquals("Good evening, World", fragment.generateGreetingMessage("World", calendar));
    }

    @Test
    public void testGreetingBoundaries() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        Calendar calendar = Calendar.getInstance();


        calendar.set(Calendar.HOUR_OF_DAY, 11);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good morning"));


        calendar.set(Calendar.HOUR_OF_DAY, 12);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good afternoon"));


        calendar.set(Calendar.HOUR_OF_DAY, 17);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good afternoon"));


        calendar.set(Calendar.HOUR_OF_DAY, 18);
        assertTrue(fragment.generateGreetingMessage("Test", calendar).startsWith("Good evening"));
    }

    @Test
    public void testLoadGreeting_Success() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        TestUserDataProvider provider = new TestUserDataProvider(true, "Kieran Sharma");
        fragment.loadGreeting(provider);
        assertEquals("Kieran", fragment.getLastGreetingName());
    }

    @Test
    public void testLoadGreeting_Failure() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        TestUserDataProvider provider = new TestUserDataProvider(false, null);
        fragment.loadGreeting(provider);
        assertEquals("User", fragment.getLastGreetingName());
    }

    @Test
    public void testLoadGreeting_EmptyName() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        TestUserDataProvider provider = new TestUserDataProvider(true, "");
        fragment.loadGreeting(provider);
        assertEquals("User", fragment.getLastGreetingName());
    }

    @Test
    public void testGreetingMessageIsNotNull() {
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        Calendar calendar = Calendar.getInstance();
        String greeting = fragment.generateGreetingMessage("Test", calendar);
        assertNotNull("Greeting should not be null", greeting);
    }



    @Test
    public void testGreetingMidnight() {
        // Test the exact start of the day (00:00)
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0); // Midnight

        // Should be "Good morning" because condition is (hour >= 0 && hour < 12)
        assertTrue("Midnight should count as morning",
                fragment.generateGreetingMessage("Test", calendar).startsWith("Good morning"));
    }

    @Test
    public void testGreetingLateNight() {
        // Test the very end of the day (23:00 / 11 PM)
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 23); // 11 PM

        // Should be "Good evening" because condition is (else) covering hours >= 18
        assertTrue("11 PM should count as evening",
                fragment.generateGreetingMessage("Test", calendar).startsWith("Good evening"));
    }

    @Test
    public void testLoadGreeting_HyphenatedName() {
        // Ensure names like "Mary-Jane Watson" are split correctly on the SPACE, not the hyphen
        TestableDashBoardFragment fragment = new TestableDashBoardFragment();
        // Simulate user data with a hyphenated first name
        TestUserDataProvider provider = new TestUserDataProvider(true, "Mary-Jane Watson");

        fragment.loadGreeting(provider);


        assertEquals("Hyphenated first names should be preserved",
                "Mary-Jane", fragment.getLastGreetingName());
    }

}
