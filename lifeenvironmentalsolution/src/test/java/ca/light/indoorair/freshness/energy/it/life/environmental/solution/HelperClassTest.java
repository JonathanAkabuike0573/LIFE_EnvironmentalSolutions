package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import org.junit.Test;
import static org.junit.Assert.*;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.HelperClass;
public class HelperClassTest {

    // 1 - Test constructor initializes values correctly
    @Test
    public void testConstructorInitializesFields() {
        HelperClass helper = new HelperClass("Alice", "alice@example.com", "1234567890");
        assertEquals("Alice", helper.getName());
        assertEquals("alice@example.com", helper.getEmail());
        assertEquals("1234567890", helper.getPhoneNumber());
    }

    // 2 - Test default constructor doesn't crash
    @Test
    public void testDefaultConstructor() {
        HelperClass helper = new HelperClass();
        assertNotNull(helper);
    }

    // 3 - Test setName()
    @Test
    public void testSetName() {
        HelperClass helper = new HelperClass();
        helper.setName("Bob");
        assertEquals("Bob", helper.getName());
    }

    // 4 - Test setEmail()
    @Test
    public void testSetEmail() {
        HelperClass helper = new HelperClass();
        helper.setEmail("bob@example.com");
        assertEquals("bob@example.com", helper.getEmail());
    }

    // 5 - Test setPhoneNumber()
    @Test
    public void testSetPhoneNumber() {
        HelperClass helper = new HelperClass();
        helper.setPhoneNumber("9876543210");
        assertEquals("9876543210", helper.getPhoneNumber());
    }

    // 6 - Test fields allow empty values
    @Test
    public void testHandlesEmptyStrings() {
        HelperClass helper = new HelperClass("", "", "");
        assertEquals("", helper.getName());
        assertEquals("", helper.getEmail());
        assertEquals("", helper.getPhoneNumber());
    }

    // 7 - Test multiple fields updated independently
    @Test
    public void testMultipleSettersWorkIndependently() {
        HelperClass helper = new HelperClass();
        helper.setName("Charlie");
        helper.setEmail("charlie@mail.com");
        helper.setPhoneNumber("1112223333");

        assertEquals("Charlie", helper.getName());
        assertEquals("charlie@mail.com", helper.getEmail());
        assertEquals("1112223333", helper.getPhoneNumber());
    }

    // 8 - Test that name is not null by default when using parameterized constructor
    @Test
    public void testNameNotNull() {
        HelperClass helper = new HelperClass("Dave", "dave@example.com", "123");
        assertNotNull(helper.getName());
    }

    // 9 - Test that email is not null by default when using parameterized constructor
    @Test
    public void testEmailNotNull() {
        HelperClass helper = new HelperClass("Eve", "eve@example.com", "456");
        assertNotNull(helper.getEmail());
    }

    // 10 - Test that updating one field does not affect others
    @Test
    public void testSetEmailDoesNotChangeName() {
        HelperClass helper = new HelperClass("Frank", "frank@example.com", "789");
        helper.setEmail("newfrank@example.com");
        
        // Email should change
        assertEquals("newfrank@example.com", helper.getEmail());
        // Name should remain the same
        assertEquals("Frank", helper.getName());
    }

    // Automation Tests

    // 11 - Test assertTrue usage
    @Test
    public void testEmailContainsAtSymbol() {
        HelperClass helper = new HelperClass("User", "test@domain.com", "123");
        assertTrue("Email should contain @ symbol", helper.getEmail().contains("@"));
    }

    // 12 - Test assertFalse usage
    @Test
    public void testNameNotEmpty() {
        HelperClass helper = new HelperClass("User", "test@domain.com", "123");
        assertFalse("Name should not be empty", helper.getName().isEmpty());
    }

    // 13 - Test assertNotEquals usage
    @Test
    public void testDifferentPhoneNumbers() {
        HelperClass helper = new HelperClass("User", "test@domain.com", "12345");
        assertNotEquals("67890", helper.getPhoneNumber());
    } }