package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

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
}
