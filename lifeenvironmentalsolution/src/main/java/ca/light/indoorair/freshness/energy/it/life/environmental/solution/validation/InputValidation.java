package ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation;

import android.util.Patterns;

public class InputValidation {

    public static boolean isValidEmail(CharSequence email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }

        String passwordPattern = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{6,}$";

        return password.matches(passwordPattern);
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && Patterns.PHONE.matcher(phone).matches();
    }

    public static boolean isValidFeedback(String feedback) {
        return feedback != null && !feedback.trim().isEmpty();
    }
}
