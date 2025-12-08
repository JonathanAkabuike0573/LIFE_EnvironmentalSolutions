package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
// Import your validation class
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.validation.InputValidation;

public class PurchasesFragment extends Fragment {

    // SharedPreferences Keys
    private static final String KEY_PURCHASED_PLAN_NAME = "purchased_plan_name";
    private static final String KEY_PURCHASED_DEVICES = "purchased_devices";

    // UI Components
    private Spinner upgradesSpinner;
    private Spinner paymentTypeSpinner;
    private TextView subtotalTextView, taxTextView, totalTextView;
    private Button confirmAndPayButton;
    private TextView cancelSubscriptionLink, currentPlanTextView, receiptLink;
    private LinearLayout paymentDetailsLayout;
    private EditText cardNameEditText, cardNumberEditText, expiryEditText, cvvEditText;

    // Data and Constants
    private static final double TAX_RATE = 0.13;
    private double currentSubtotal = 0.0;
    private String selectedPaymentMethod = "";
    private String currentActivePlan = null;
    private final Map<String, Integer> planDeviceLimits = new HashMap<>();
    private final Map<String, Double> upgradePrices = new HashMap<>();
    private final List<String> deviceOptions = Arrays.asList("Thermostat", "Air Conditioner", "Smart TV", "Smart Light", "Presence Sensor", "Air Quality Monitor");
    private final List<String> bankOptions = Arrays.asList("CIBC", "TD", "RBC", "Scotiabank", "BMO");
    private List<String> finalSelectedDevices = new ArrayList<>();

    public PurchasesFragment() { /* Required empty public constructor */ }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializePlans();
    }

    private void initializePlans() {
        upgradePrices.put("Basic Plan (Monthly)", 9.99);
        upgradePrices.put("Pro Plan (Quarterly)", 24.99);
        upgradePrices.put("Premium (Annual)", 79.99);

        // Define device limits for each plan
        planDeviceLimits.put("Basic Plan (Monthly)", 2);
        planDeviceLimits.put("Pro Plan (Quarterly)", 4);
        planDeviceLimits.put("Premium (Annual)", 6);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_purchases, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        loadAndDisplayCurrentPlan();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews(@NonNull View view) {
        upgradesSpinner = view.findViewById(R.id.spinner_upgrades);
        paymentTypeSpinner = view.findViewById(R.id.spinner_payment_type);
        subtotalTextView = view.findViewById(R.id.text_subtotal);
        taxTextView = view.findViewById(R.id.text_tax);
        totalTextView = view.findViewById(R.id.text_total);
        confirmAndPayButton = view.findViewById(R.id.button_confirm_and_pay);
        cancelSubscriptionLink = view.findViewById(R.id.text_cancel_subscription_link);
        currentPlanTextView = view.findViewById(R.id.text_current_plan);
        paymentDetailsLayout = view.findViewById(R.id.layout_payment_details);
        receiptLink = view.findViewById(R.id.text_view_receipts_link);

        // Initialize EditTexts for validation
        cardNameEditText = view.findViewById(R.id.edit_text_card_name);
        cardNumberEditText = view.findViewById(R.id.edit_text_card_number);
        expiryEditText = view.findViewById(R.id.edit_text_expiry);
        cvvEditText = view.findViewById(R.id.edit_text_cvv);
    }

    private void loadAndDisplayCurrentPlan() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        currentActivePlan = prefs.getString(KEY_PURCHASED_PLAN_NAME, null);

        if (currentActivePlan != null) {
            Set<String> purchasedDevices = prefs.getStringSet(KEY_PURCHASED_DEVICES, new HashSet<>());
            String devicesText = purchasedDevices.isEmpty() ? getString(R.string.no_extra_devices) : getString(R.string.devices) + String.join(", ", purchasedDevices);
            currentPlanTextView.setText(getString(R.string.current_plan) + currentActivePlan + "\n" + devicesText);
            currentPlanTextView.setVisibility(View.VISIBLE);
            cancelSubscriptionLink.setVisibility(View.VISIBLE);
        } else {
            currentPlanTextView.setVisibility(View.GONE);
            cancelSubscriptionLink.setVisibility(View.GONE);
        }
    }

    private boolean isPlanPurchased(String planName) {
        return planName.equals(currentActivePlan);
    }

    private void setupSpinners() {
        // Upgrades Spinner
        List<String> currentUpgradeDisplayOptions = new ArrayList<>();
        currentUpgradeDisplayOptions.add(getString(R.string.select_an_upgrade));
        currentUpgradeDisplayOptions.addAll(upgradePrices.keySet().stream()
                .map(key -> key + " ($" + String.format(Locale.getDefault(), "%.2f", upgradePrices.get(key)) + ")")
                .collect(Collectors.toList()));
        ArrayAdapter<String> upgradeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, currentUpgradeDisplayOptions);
        upgradesSpinner.setAdapter(upgradeAdapter);
        upgradesSpinner.setSelection(0, false);

        upgradesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    currentSubtotal = 0.0;
                    updatePriceDisplay(currentSubtotal);
                    return;
                }
                String selectedItemText = parent.getItemAtPosition(position).toString();
                String originalKey = getOriginalKey(selectedItemText);

                if (isPlanPurchased(originalKey)) {
                    Toast.makeText(getContext(), R.string.you_have_already_purchased_this_plan , Toast.LENGTH_LONG).show();
                    upgradesSpinner.setSelection(0);
                    return;
                }

                currentSubtotal = upgradePrices.getOrDefault(originalKey, 0.00);
                updatePriceDisplay(currentSubtotal);
                // Get the device limit for the selected plan
                int maxDevices = planDeviceLimits.getOrDefault(originalKey, 0);
                showDeviceSelectionDialog(originalKey, maxDevices);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Payment Type Spinner
        String[] paymentTypes = {"Select Payment Type...", "Visa/Mastercard", "Amex", "PayPal", "Google Pay"};
        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, paymentTypes);
        paymentTypeSpinner.setAdapter(paymentAdapter);

        paymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = parent.getItemAtPosition(position).toString();
                if (position == 0) {
                    paymentDetailsLayout.setVisibility(View.GONE);
                } else if (selectedPaymentMethod.equals(getString(R.string.visa_mastercard))) {
                    showBankSelectionDialog();
                } else {
                    paymentDetailsLayout.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupClickListeners() {
        confirmAndPayButton.setOnClickListener(v -> {
            if (!isFormValid()) {
                return;
            }
            savePurchase();
            showPaymentSuccessfulDialog();
        });

        cancelSubscriptionLink.setOnClickListener(v -> showCancelDialog());
        receiptLink.setOnClickListener(v -> showEmailReceiptDialog());
    }

    private boolean isFormValid() {
        if (currentSubtotal <= 0.00) {
            Toast.makeText(getContext(), R.string.please_select_a_valid_upgrade_option , Toast.LENGTH_SHORT).show();
            return false;
        }
        if (paymentTypeSpinner.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), R.string.please_select_a_payment_method , Toast.LENGTH_SHORT).show();
            return false;
        }
        // If payment details are visible, validate them
        if (paymentDetailsLayout.getVisibility() == View.VISIBLE) {
            String cardName = cardNameEditText.getText().toString();
            String cardNumber = cardNumberEditText.getText().toString();
            String expiry = expiryEditText.getText().toString();
            String cvv = cvvEditText.getText().toString();

            if (!InputValidation.isValidCardHolderName(cardName)) {
                cardNameEditText.setError(getString(R.string.please_enter_a_valid_name1));
                return false;
            }
            if (!InputValidation.isValidCardNumber(cardNumber)) {
                cardNumberEditText.setError(getString(R.string.invalid_card_number));
                return false;
            }
            if (!InputValidation.isValidExpiryDate(expiry)) {
                expiryEditText.setError(getString(R.string.invalid_expiry_date_mm_yy));
                return false;
            }
            if (!InputValidation.isValidCvv(cvv)) {
                cvvEditText.setError(getString(R.string.invalid_cvv));
                return false;
            }
        }
        return true;
    }

    private void savePurchase() {
        if (upgradesSpinner.getSelectedItemPosition() == 0) return;
        String selectedPlan = getOriginalKey(upgradesSpinner.getSelectedItem().toString());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit()
                .putString(KEY_PURCHASED_PLAN_NAME, selectedPlan)
                .putStringSet(KEY_PURCHASED_DEVICES, new HashSet<>(finalSelectedDevices))
                .apply();
        loadAndDisplayCurrentPlan();
    }

    private void cancelPurchase() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit()
                .remove(KEY_PURCHASED_PLAN_NAME)
                .remove(KEY_PURCHASED_DEVICES)
                .apply();
        loadAndDisplayCurrentPlan();
        upgradesSpinner.setSelection(0);
        Toast.makeText(getContext(), R.string.your_upgrade_has_been_canceled, Toast.LENGTH_LONG).show();
    }

    private String getOriginalKey(String selectedItemText) {
        int priceStart = selectedItemText.lastIndexOf(" ($");
        return (priceStart > 0) ? selectedItemText.substring(0, priceStart) : selectedItemText;
    }

    private void updatePriceDisplay(double subtotal) {
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        subtotalTextView.setText(getString(R.string.subtotal) + currencyFormat.format(subtotal));
        taxTextView.setText(String.format(Locale.US, getString(R.string.tax_0f_s), TAX_RATE * 100, currencyFormat.format(tax)));
        totalTextView.setText(getString(R.string.total) + currencyFormat.format(total));
        confirmAndPayButton.setText(getString(R.string.continue_and_pay) + currencyFormat.format(total));
    }



    private void showDeviceSelectionDialog(String originalPlanName, int maxDevices) {
        final CharSequence[] items = deviceOptions.toArray(new CharSequence[0]);
        final boolean[] checkedItems = new boolean[deviceOptions.size()];
        final List<String> selectedDevices = new ArrayList<>();

        new AlertDialog.Builder(requireContext())
                .setTitle(originalPlanName + getString(R.string.devices_select_up_to) + maxDevices + ")")
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        if (selectedDevices.size() >= maxDevices) {

                            ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                            Toast.makeText(getContext(), getString(R.string.device_limit_of) + maxDevices + getString(R.string.reached), Toast.LENGTH_SHORT).show();
                        } else {
                            selectedDevices.add(deviceOptions.get(which));
                        }
                    } else {
                        selectedDevices.remove(deviceOptions.get(which));
                    }
                })
                .setPositiveButton(R.string.ok , (dialog, which) -> {
                    finalSelectedDevices = new ArrayList<>(selectedDevices);
                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    upgradesSpinner.setSelection(0);
                    dialog.dismiss();
                })
                .show();
    }

    private void showBankSelectionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.select_your_issuing_bank)
                .setItems(bankOptions.toArray(new CharSequence[0]), (dialog, which) -> {
                    String selectedBank = bankOptions.get(which);
                    selectedPaymentMethod = getString(R.string.visa_mastercard) + selectedBank + ")";
                    paymentDetailsLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), selectedBank + getString(R.string.card_selected_enter_details_below), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> paymentTypeSpinner.setSelection(0))
                .show();
    }

    private void showCancelDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cancel_upgrade)
                .setMessage(R.string.are_you_sure_you_want_to_cancel_your_current_active_plan)
                .setPositiveButton(R.string.yes_cancel , (dialog, which) -> cancelPurchase())
                .setNegativeButton(R.string.no_keep_it , null)
                .setIcon(R.drawable.logolife)
                .show();
    }

    private void showEmailReceiptDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.email_receipt)
                .setMessage(R.string.do_you_want_an_email_copy_of_your_receipt)
                .setPositiveButton(R.string.yes_email_it, (dialog, which) -> Toast.makeText(getContext(), R.string.receipt_will_be_emailed_shortly, Toast.LENGTH_SHORT).show())
                .setNegativeButton(R.string.no_thanks , null)
                .setIcon(R.drawable.logolife)
                .show();
    }

    private void showPaymentSuccessfulDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.payment_successful)
                .setMessage(R.string.your_upgrade_is_now_active_thank_you_for_your_purchase)
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.logolife)
                .show();
    }
}
