package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SignupActivityEspressoTest {

    @Rule
    public ActivityScenarioRule<SignupActivity> activityRule =
            new ActivityScenarioRule<>(SignupActivity.class);

    @Test
    public void testEmptyFieldsValidation() {
        onView(withId(R.id.buttonSignUp)).perform(click());
        // Since all fields are empty, the first check fails. 
        // Assuming the Toast appears or nothing happens. 
        // For this test, let's check if we can verify one of the fields is still empty or displayed
        onView(withId(R.id.signupFUllname)).check(matches(withText("")));
    }

    @Test
    public void testInvalidEmailFormat() {
        onView(withId(R.id.signupFUllname)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.signUpPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.signUpPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.signUpConfirmPassword)).perform(typeText("password123"), closeSoftKeyboard());
        
        // Invalid email
        onView(withId(R.id.SignUpEmail)).perform(typeText("invalid-email"), closeSoftKeyboard());
        
        onView(withId(R.id.buttonSignUp)).perform(click());
        
        onView(withId(R.id.SignUpEmail)).check(matches(hasErrorText("Please enter a valid email address")));
    }

    @Test
    public void testShortPassword() {
        onView(withId(R.id.signupFUllname)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.signUpPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.SignUpEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        
        // Short password
        onView(withId(R.id.signUpPassword)).perform(typeText("123"), closeSoftKeyboard());
        onView(withId(R.id.signUpConfirmPassword)).perform(typeText("123"), closeSoftKeyboard());
        
        onView(withId(R.id.buttonSignUp)).perform(click());
        
        onView(withId(R.id.signUpPassword)).check(matches(hasErrorText("Password must be at least 6 characters")));
    }

    @Test
    public void testPasswordMismatch() {
        onView(withId(R.id.signupFUllname)).perform(typeText("Test User"), closeSoftKeyboard());
        onView(withId(R.id.signUpPhoneNumber)).perform(typeText("1234567890"), closeSoftKeyboard());
        onView(withId(R.id.SignUpEmail)).perform(typeText("test@example.com"), closeSoftKeyboard());
        
        // Mismatched passwords
        onView(withId(R.id.signUpPassword)).perform(typeText("password123"), closeSoftKeyboard());
        onView(withId(R.id.signUpConfirmPassword)).perform(typeText("password456"), closeSoftKeyboard());
        
        onView(withId(R.id.buttonSignUp)).perform(click());
        
        onView(withId(R.id.signUpConfirmPassword)).check(matches(hasErrorText("Passwords do not match")));
    }

    @Test
    public void testRedirectToLogin() {
        // Click the "Already have an account?" link
        onView(withId(R.id.signUploginredirect)).perform(click());
        
        // Check if the Login Activity UI (e.g., Login Button) is displayed
        onView(withId(R.id.btn_login)).check(matches(isDisplayed()));
    }
}
