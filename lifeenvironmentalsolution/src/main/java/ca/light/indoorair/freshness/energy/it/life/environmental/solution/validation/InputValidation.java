package ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation;

import android.util.Patterns;
import java.util.Calendar;

public class InputValidation {

    public static boolean isValidEmail(CharSequence email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        // This regex requires at least one uppercase, one number, one special character
        String passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,}$";
        return password.matches(passwordPattern);
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean isValidPhone(String phone) {
        // Using a more lenient regex for phone numbers as Patterns.PHONE can be strict
        return phone != null && phone.matches("^[+]?[0-9]{10,13}$");
    }

    public static boolean isValidFeedback(String feedback) {
        return feedback != null && !feedback.trim().isEmpty();
    }

    // --- NEW CREDIT CARD VALIDATION METHODS ---

    /**
     * Validates the name on the credit card.
     * @param cardHolderName The name entered by the user.
     * @return true if the name is not empty, false otherwise.
     */
    public static boolean isValidCardHolderName(String cardHolderName) {
        return cardHolderName != null && !cardHolderName.trim().isEmpty();
    }

    /**
     * Validates a credit card number using the Luhn algorithm (mod 10 check).
     * @param cardNumber The card number string.
     * @return true if the card number is valid according to the Luhn algorithm.
     */
    public static boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }

        int sum = 0;
        boolean alternate = false;
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Validates the credit card expiry date.
     * Checks format (MM/YY) and ensures the date is not in the past.
     * @param expiryDate The expiry date string (e.g., "12/28").
     * @return true if the format is correct and the date is valid.
     */
    public static boolean isValidExpiryDate(String expiryDate) {
        if (expiryDate == null || !expiryDate.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return false;
        }

        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR) % 100; // Get last two digits of the year
        int currentMonth = calendar.get(Calendar.MONTH) + 1; // Calendar month is 0-indexed

        if (year < currentYear || (year == currentYear && month < currentMonth)) {
            return false; // Card has expired
        }

        return true;
    }

    /**
     * Validates the CVV (Card Verification Value).
     * @param cvv The CVV string.
     * @return true if the CVV is 3 or 4 digits long.
     */
    public static boolean isValidCvv(String cvv) {
        return cvv != null && cvv.matches("^[0-9]{3,4}$");
    }
}
