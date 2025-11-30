package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class PurchasesFragment extends Fragment {

    // UI Components
    private Spinner upgradesSpinner;
    private Spinner paymentTypeSpinner;
    private TextView subtotalTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private Button confirmAndPayButton;
    private TextView cancelSubscriptionLink;

    // UI Components for Payment Details
    private LinearLayout paymentDetailsLayout;
    private EditText cardNameEditText;
    private EditText cardNumberEditText;

    // Data and Constants
    private static final double TAX_RATE = 0.13; // 13% tax rate
    private double currentSubtotal = 0.0;
    private String selectedPaymentMethod = "";

    // *** Plan Limits Map ***
    private final Map<String, Integer> planDeviceLimits = new HashMap<>();

    // Map to store upgrade names and their base prices
    private final Map<String, Double> upgradePrices = new HashMap<>();

    // Available device options
    private final List<String> deviceOptions = Arrays.asList(
            "Air Quality", "Smart Light", "Thermostat", "Air Conditioner", "Presence Sensor", "Smart TV"
    );

    private final List<String> bankOptions = Arrays.asList(
            "CIBC", "TD", "RBC", "Scotiabank", "BMO"
    );

    // This list will hold the options *displayed* in the spinner
    private List<String> currentUpgradeDisplayOptions;
    private ArrayAdapter<String> upgradeAdapter;


    public PurchasesFragment() {
        // *** MODIFIED: Removed "Select Upgrade" and "Lifetime Access" ***
        // Initialize upgrade prices
        upgradePrices.put("Basic Plan (Monthly)", 9.99);
        upgradePrices.put("Pro Plan (Quarterly)", 24.99);
        upgradePrices.put("Premium (Annual)", 79.99);

        // *** NEW: Initialize plan device limits ***
        planDeviceLimits.put("Basic Plan (Monthly)", 2);
        planDeviceLimits.put("Pro Plan (Quarterly)", 4);
        planDeviceLimits.put("Premium (Annual)", 6);
    }

    public static PurchasesFragment newInstance() {
        PurchasesFragment fragment = new PurchasesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_purchases, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize UI components
        upgradesSpinner = view.findViewById(R.id.spinner_upgrades);
        paymentTypeSpinner = view.findViewById(R.id.spinner_payment_type);
        subtotalTextView = view.findViewById(R.id.text_subtotal);
        taxTextView = view.findViewById(R.id.text_tax);
        totalTextView = view.findViewById(R.id.text_total);
        confirmAndPayButton = view.findViewById(R.id.button_confirm_and_pay);
        cancelSubscriptionLink = view.findViewById(R.id.text_cancel_subscription_link);

        // Initialize Payment Detail components
        paymentDetailsLayout = view.findViewById(R.id.layout_payment_details);
        cardNameEditText = view.findViewById(R.id.edit_text_card_name);
        cardNumberEditText = view.findViewById(R.id.edit_text_card_number);

        setupSpinners();
        setupClickListeners(view);
    }

    /**
     * Extracts the original map key by stripping the appended price and the selected devices from the formatted spinner text.
     */
    private String getOriginalKey(String selectedItemText) {
        // Find the index of the plan price/device info
        int priceStart = selectedItemText.lastIndexOf(" ($");
        String baseText = (priceStart > 0) ? selectedItemText.substring(0, priceStart) : selectedItemText;

        int deviceStart = baseText.lastIndexOf(" (");
        if (deviceStart > 0) {
            if (baseText.substring(deviceStart).contains("device(s) selected")) {
                return baseText.substring(0, deviceStart);
            }
        }

        return baseText;
    }

    /**
     * Sets up the data and listeners for the two Spinner dropdowns.
     */
    private void setupSpinners() {
        // 1. Upgrades Spinner (with Price)
        // Only include options that have a price (all remaining options do)
        currentUpgradeDisplayOptions = upgradePrices.keySet().stream()
                .map(key -> key + " ($" + String.format(Locale.getDefault(), "%.2f", upgradePrices.get(key)) + ")")
                .collect(ArrayList::new, List::add, List::addAll);

        upgradeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, currentUpgradeDisplayOptions);
        upgradesSpinner.setAdapter(upgradeAdapter);

        // *** MODIFIED: Set Default Selection to the first plan (Basic Plan) ***
        final int defaultSelectionIndex = 0;

        upgradesSpinner.post(() -> {
            upgradesSpinner.setSelection(defaultSelectionIndex);
            // Calculate price for the first plan immediately
            double initialPrice = upgradePrices.get(getOriginalKey(currentUpgradeDisplayOptions.get(defaultSelectionIndex)));
            currentSubtotal = initialPrice;
            updatePriceDisplay(currentSubtotal);
        });

        // --- Listener for subsequent selections ---
        upgradesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialLoad = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = parent.getItemAtPosition(position).toString();
                String originalKey = getOriginalKey(selectedItemText);

                if (isInitialLoad && position == defaultSelectionIndex) {
                    isInitialLoad = false;
                    // Skip showing dialog on initial load, but update price
                    double initialPrice = upgradePrices.getOrDefault(originalKey, 0.00);
                    currentSubtotal = initialPrice;
                    updatePriceDisplay(currentSubtotal);
                    return;
                }

                double planPrice = upgradePrices.getOrDefault(originalKey, 0.00);
                int maxDevices = planDeviceLimits.getOrDefault(originalKey, 0); // Get dynamic limit

                if (planPrice > 0.00) {
                    currentSubtotal = planPrice;
                    // Show the device selection dialog with the specific max limit
                    showDeviceSelectionDialog(originalKey, position, maxDevices);
                } else {
                    currentSubtotal = 0.00;
                    updatePriceDisplay(currentSubtotal);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // 2. Payment Type Spinner
        String[] paymentTypes = {"Select Payment Type", "Visa/Mastercard", "Amex", "PayPal", "Google Pay"};
        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, paymentTypes);
        paymentTypeSpinner.setAdapter(paymentAdapter);

        paymentTypeSpinner.setSelection(0);

        paymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = parent.getItemAtPosition(position).toString();

                if (selectedPaymentMethod.equals("Visa/Mastercard")) {
                    showBankSelectionDialog();
                    paymentDetailsLayout.setVisibility(View.GONE);
                } else if (position > 0) { // If any other payment method is selected (Amex, PayPal, Google Pay)
                    paymentDetailsLayout.setVisibility(View.VISIBLE);
                } else { // "Select Payment Type"
                    paymentDetailsLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    /**
     * Shows a multi-choice dialog for the user to select devices, respecting a max limit.
     * @param originalPlanName The name of the selected plan.
     * @param originalPosition The position of the selected plan in the spinner.
     * @param maxDevices The maximum number of devices allowed for this plan.
     */
    private void showDeviceSelectionDialog(String originalPlanName, int originalPosition, int maxDevices) {
        if (getContext() == null) return;

        final CharSequence[] items = deviceOptions.toArray(new CharSequence[0]);
        // Re-initialize checkedItems based on the current display text if possible,
        // but for simplicity here, we'll start with an empty selection on every dialog open.
        final boolean[] checkedItems = new boolean[deviceOptions.size()];
        final List<String> selectedDevices = new ArrayList<>();

        new AlertDialog.Builder(getContext())
                // ⭐ UPDATED: Title reflects the plan-specific max devices
                .setTitle(originalPlanName + " Devices (Max " + maxDevices + ")")
                .setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            // *** NEW LOGIC: Check if plan-specific max limit is reached ***
                            if (selectedDevices.size() >= maxDevices) {
                                // If max reached, display custom message and uncheck visually
                                checkedItems[which] = false;
                                ((AlertDialog) dialog).getListView().setItemChecked(which, false);
                                // Show dialog message: "can't select, max is reached"
                                showMaxDeviceReachedDialog(maxDevices);
                            } else {
                                selectedDevices.add(deviceOptions.get(which));
                            }
                        } else {
                            selectedDevices.remove(deviceOptions.get(which));
                        }
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateUpgradeSpinnerDisplay(originalPlanName, selectedDevices, originalPosition);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Reset selection to the current plan to maintain context
                        // Note: If the user previously had 3 devices selected and cancels,
                        // this simply keeps the current plan selected but doesn't retain device state.
                        upgradesSpinner.setSelection(originalPosition);
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Shows the dialog when the device selection limit is reached.
     */
    private void showMaxDeviceReachedDialog(int maxDevices) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Device Limit Reached")
                .setMessage("Can't select more devices. The limit for this plan is **" + maxDevices + "** devices.")
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Shows a dialog for the user to select one of the five banks. (Unchanged)
     */
    private void showBankSelectionDialog() {
        if (getContext() == null) return;

        final CharSequence[] banks = bankOptions.toArray(new CharSequence[0]);

        new AlertDialog.Builder(getContext())
                .setTitle("Select Your Issuing Bank")
                .setItems(banks, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String selectedBank = bankOptions.get(which);
                        selectedPaymentMethod = "Visa/Mastercard (" + selectedBank + ")";
                        paymentDetailsLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(),
                                selectedBank + " Card selected. Enter details below.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        paymentTypeSpinner.setSelection(0);
                        paymentDetailsLayout.setVisibility(View.GONE);
                        selectedPaymentMethod = "";
                    }
                })
                .setCancelable(false)
                .show();
    }


    /**
     * Updates the Spinner's displayed text after devices are selected.
     */
    private void updateUpgradeSpinnerDisplay(String originalPlanName, List<String> selectedDevices, int positionToSet) {
        if (upgradeAdapter == null) return;

        String deviceSummary = selectedDevices.isEmpty()
                ? ""
                : " (" + selectedDevices.size() + " device(s) selected)";

        String priceText = upgradePrices.get(originalPlanName) > 0
                ? " ($" + String.format(Locale.getDefault(), "%.2f", upgradePrices.get(originalPlanName)) + ")"
                : "";

        String newDisplayText = originalPlanName + deviceSummary + priceText;

        currentUpgradeDisplayOptions.set(positionToSet, newDisplayText);
        upgradeAdapter.notifyDataSetChanged();
        upgradesSpinner.setSelection(positionToSet);
        updatePriceDisplay(currentSubtotal);
    }

    /**
     * Recalculates tax and total, then updates the summary TextViews and the button text.
     */
    private void updatePriceDisplay(double subtotal) {
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        subtotalTextView.setText("Subtotal: " + currencyFormat.format(subtotal));
        taxTextView.setText("Tax (" + String.format(Locale.getDefault(), "%.0f%%", TAX_RATE * 100) + "): " + currencyFormat.format(tax));
        totalTextView.setText("Total: " + currencyFormat.format(total));

        confirmAndPayButton.setText("Continue and Pay " + currencyFormat.format(total));
    }

    /**
     * Displays an AlertDialog to confirm upgrade cancellation.
     */
    private void showCancelDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Upgrade")
                .setMessage("Do you want to cancel your upgrade?")
                .setPositiveButton("Yes, Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), "Your Upgrade will be canceled in 10 business days.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("No, Keep It", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), "Upgrade maintained.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setIcon(R.drawable.logolife)
                .show();
    }

    /**
     * Shows the AlertDialog asking the user if they want an email copy of the receipt.
     */
    private void showEmailReceiptDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Email Receipt")
                .setMessage("Do you want an email copy of your receipt?")
                .setPositiveButton("Yes, Email It", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getContext(), "Receipt copy will be emailed shortly.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No, Thanks", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // User chose not to receive an email
                    }
                })
                .setIcon(R.drawable.logolife)
                .show();
    }

    /**
     * Shows the Payment Successful dialog.
     */
    private void showPaymentSuccessfulDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Payment Successful! 🎉")
                .setMessage("Your payment was processed successfully. Thank you for your purchase!")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.logolife)
                .show();
    }

    /**
     * Sets up click listeners for the buttons and links.
     */
    private void setupClickListeners(@NonNull View view) {
        // "Confirm and Pay" button
        confirmAndPayButton.setOnClickListener(v -> {
            if (currentSubtotal <= 0.00) {
                Toast.makeText(getContext(), "Please select a valid upgrade option to continue.", Toast.LENGTH_LONG).show();
                return;
            }
            if (paymentTypeSpinner.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Please select a payment type to continue.", Toast.LENGTH_LONG).show();
                return;
            }

            // 1. Display Toast: "payment is in process"
            Toast.makeText(getContext(),
                    "Payment is in process for: " + selectedPaymentMethod,
                    Toast.LENGTH_LONG).show();

            // 2. Display Alert Dialog: "Payment successful"
            showPaymentSuccessfulDialog();
        });

        // "Do you want an email copy of receipt?" link
        TextView viewReceiptsLink = view.findViewById(R.id.text_view_receipts_link);
        if (viewReceiptsLink != null) {
            viewReceiptsLink.setOnClickListener(v -> {
                showEmailReceiptDialog();
            });
        }

        // "Cancel Upgrade" link
        if (cancelSubscriptionLink != null) {
            cancelSubscriptionLink.setOnClickListener(v -> {
                showCancelDialog();
            });
        }
    }
}