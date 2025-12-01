package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
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
import java.util.HashSet;
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

    // *** MODIFIED: Only listing the devices that cost money ***
    private final List<String> deviceOptions = Arrays.asList(
            "Thermostat", "Air Conditioner", "Smart TV"
    );

    // Store the devices currently selected by the user so we can save them later
    private List<String> finalSelectedDevices = new ArrayList<>();

    private final List<String> bankOptions = Arrays.asList(
            "CIBC", "TD", "RBC", "Scotiabank", "BMO"
    );

    // This list will hold the options *displayed* in the spinner
    private List<String> currentUpgradeDisplayOptions;
    private ArrayAdapter<String> upgradeAdapter;


    public PurchasesFragment() {
        // Initialize upgrade prices
        upgradePrices.put("Basic Plan (Monthly)", 9.99);
        upgradePrices.put("Pro Plan (Quarterly)", 24.99);
        upgradePrices.put("Premium (Annual)", 79.99);

        // Initialize plan device limits
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
     * Extracts the original map key by stripping the appended price and the selected devices.
     */
    private String getOriginalKey(String selectedItemText) {
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

    private void setupSpinners() {
        // 1. Upgrades Spinner
        currentUpgradeDisplayOptions = upgradePrices.keySet().stream()
                .map(key -> key + " ($" + String.format(Locale.getDefault(), "%.2f", upgradePrices.get(key)) + ")")
                .collect(ArrayList::new, List::add, List::addAll);

        upgradeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, currentUpgradeDisplayOptions);
        upgradesSpinner.setAdapter(upgradeAdapter);

        final int defaultSelectionIndex = 0;

        upgradesSpinner.post(() -> {
            upgradesSpinner.setSelection(defaultSelectionIndex);
            double initialPrice = upgradePrices.get(getOriginalKey(currentUpgradeDisplayOptions.get(defaultSelectionIndex)));
            currentSubtotal = initialPrice;
            updatePriceDisplay(currentSubtotal);
        });

        upgradesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isInitialLoad = true;
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = parent.getItemAtPosition(position).toString();
                String originalKey = getOriginalKey(selectedItemText);

                if (isInitialLoad && position == defaultSelectionIndex) {
                    isInitialLoad = false;
                    double initialPrice = upgradePrices.getOrDefault(originalKey, 0.00);
                    currentSubtotal = initialPrice;
                    updatePriceDisplay(currentSubtotal);
                    return;
                }

                double planPrice = upgradePrices.getOrDefault(originalKey, 0.00);
                int maxDevices = planDeviceLimits.getOrDefault(originalKey, 0);

                if (planPrice > 0.00) {
                    currentSubtotal = planPrice;
                    showDeviceSelectionDialog(originalKey, position, maxDevices);
                } else {
                    currentSubtotal = 0.00;
                    updatePriceDisplay(currentSubtotal);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
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
                } else if (position > 0) {
                    paymentDetailsLayout.setVisibility(View.VISIBLE);
                } else {
                    paymentDetailsLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void showDeviceSelectionDialog(String originalPlanName, int originalPosition, int maxDevices) {
        if (getContext() == null) return;

        final CharSequence[] items = deviceOptions.toArray(new CharSequence[0]);
        final boolean[] checkedItems = new boolean[deviceOptions.size()];
        final List<String> selectedDevices = new ArrayList<>();

        new AlertDialog.Builder(getContext())
                .setTitle(originalPlanName + " Devices (Max " + maxDevices + ")")
                .setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (isChecked) {
                            if (selectedDevices.size() >= maxDevices) {
                                checkedItems[which] = false;
                                ((AlertDialog) dialog).getListView().setItemChecked(which, false);
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
                        // *** SAVE SELECTION TO CLASS VARIABLE ***
                        finalSelectedDevices = new ArrayList<>(selectedDevices);

                        updateUpgradeSpinnerDisplay(originalPlanName, selectedDevices, originalPosition);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        upgradesSpinner.setSelection(originalPosition);
                        dialog.dismiss();
                    }
                })
                .setCancelable(true)
                .show();
    }

    private void showMaxDeviceReachedDialog(int maxDevices) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Device Limit Reached")
                .setMessage("Can't select more devices. The limit for this plan is **" + maxDevices + "** devices.")
                .setPositiveButton("OK", null)
                .show();
    }

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

    private void updatePriceDisplay(double subtotal) {
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        subtotalTextView.setText("Subtotal: " + currencyFormat.format(subtotal));
        taxTextView.setText("Tax (" + String.format(Locale.getDefault(), "%.0f%%", TAX_RATE * 100) + "): " + currencyFormat.format(tax));
        totalTextView.setText("Total: " + currencyFormat.format(total));

        confirmAndPayButton.setText("Continue and Pay " + currencyFormat.format(total));
    }

    private void showCancelDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Cancel Upgrade")
                .setMessage("Do you want to cancel your upgrade?")
                .setPositiveButton("Yes, Cancel", (dialog, which) ->
                        Toast.makeText(getContext(), "Your Upgrade will be canceled in 10 business days.", Toast.LENGTH_LONG).show())
                .setNegativeButton("No, Keep It", (dialog, which) ->
                        Toast.makeText(getContext(), "Upgrade maintained.", Toast.LENGTH_SHORT).show())
                .setIcon(R.drawable.logolife)
                .show();
    }

    private void showEmailReceiptDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Email Receipt")
                .setMessage("Do you want an email copy of your receipt?")
                .setPositiveButton("Yes, Email It", (dialog, which) ->
                        Toast.makeText(getContext(), "Receipt copy will be emailed shortly.", Toast.LENGTH_SHORT).show())
                .setNegativeButton("No, Thanks", null)
                .setIcon(R.drawable.logolife)
                .show();
    }

    private void showPaymentSuccessfulDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Payment Successful! 🎉")
                .setMessage("Your payment was processed successfully. Thank you for your purchase!")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.logolife)
                .show();
    }

    private void setupClickListeners(@NonNull View view) {

        confirmAndPayButton.setOnClickListener(v -> {
            if (currentSubtotal <= 0.00) {
                Toast.makeText(getContext(), "Please select a valid upgrade option to continue.", Toast.LENGTH_LONG).show();
                return;
            }
            if (paymentTypeSpinner.getSelectedItemPosition() == 0) {
                Toast.makeText(getContext(), "Please select a payment type to continue.", Toast.LENGTH_LONG).show();
                return;
            }


            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();


            editor.putBoolean("has_paid_subscription", true);

            editor.putStringSet("allowed_devices", new HashSet<>(finalSelectedDevices));
            editor.apply();

            Toast.makeText(getContext(), "Payment Processed! Access granted.", Toast.LENGTH_LONG).show();
            showPaymentSuccessfulDialog();
        });

        TextView viewReceiptsLink = view.findViewById(R.id.text_view_receipts_link);
        if (viewReceiptsLink != null) {
            viewReceiptsLink.setOnClickListener(v -> showEmailReceiptDialog());
        }

        if (cancelSubscriptionLink != null) {
            cancelSubscriptionLink.setOnClickListener(v -> showCancelDialog());
        }
    }
}
