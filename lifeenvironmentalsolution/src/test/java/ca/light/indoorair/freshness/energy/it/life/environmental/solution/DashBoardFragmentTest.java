package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;

// Dummy UserDataProvider for compilation
interface UserDataProvider {
    interface UserDataCallback {
        void onDataReceived(String userName);
        void onError(String errorMessage);
    }
    void fetchUserData(UserDataCallback callback);
}

// Dummy FakeUserDataProvider for compilation
class FakeUserDataProvider implements UserDataProvider {
    private final boolean shouldSucceed;
    private final String nameToReturn;

    public FakeUserDataProvider(boolean shouldSucceed, String nameToReturn) {
        this.shouldSucceed = shouldSucceed;
        this.nameToReturn = nameToReturn;
    }

    @Override
    public void fetchUserData(UserDataCallback callback) {
        if (shouldSucceed) {
            callback.onDataReceived(nameToReturn);
        } else {
            callback.onError("Failure in retrieving the  user data.");
        }
    }
}

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
}
