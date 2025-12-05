package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.FeedBackPage;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 36)
public class FeedBackPageTest {

    @Test
    public void feedbackSubmission_showsConfirmationToast() {
        // Launch the fragment in a container
        try (FragmentScenario<FeedBackPage> scenario = FragmentScenario.launchInContainer(
                FeedBackPage.class,
                null, // No fragment arguments
                R.style.Theme_LIFE_EnvironmentalSolution, // App theme
                (FragmentFactory) null)) {

            // Type feedback into the EditText
            onView(withId(R.id.etFeedback)).perform(typeText("This is a test feedback."));

            // Click the submit button
            onView(withId(R.id.btnSend)).perform(click());

            // Verify that the correct toast message is shown
            assertEquals("Feedback Submitted", ShadowToast.getTextOfLatestToast());
        }
    }
}
