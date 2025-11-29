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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.text.NumberFormat;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class PurchasesFragment extends Fragment {

    // UI Components
    private Spinner freeItemsSpinner;
    private Spinner upgradesSpinner;
    private Spinner paymentTypeSpinner;
    private TextView subtotalTextView;
    private TextView taxTextView;
    private TextView totalTextView;
    private Button confirmAndPayButton;
    private TextView cancelSubscriptionLink;

    // Data and Constants
    private static final double TAX_RATE = 0.13; // 13% tax rate
    private double currentSubtotal = 0.0;
    private String selectedPaymentMethod = "";

    // Map to store upgrade names and their base prices
    private final Map<String, Double> upgradePrices = new HashMap<>();

    public PurchasesFragment() {
        // Initialize upgrade prices
        upgradePrices.put("Select Upgrade", 0.00);
        upgradePrices.put("Basic Plan (Monthly)", 9.99);
        upgradePrices.put("Pro Plan (Quarterly)", 24.99);
        upgradePrices.put("Premium (Annual)", 79.99);
        upgradePrices.put("Lifetime Access", 199.99);
    }

    public static PurchasesFragment newInstance() {
        PurchasesFragment fragment = new PurchasesFragment();
        // Standard bundle setup
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
        freeItemsSpinner = view.findViewById(R.id.spinner_free_items);
        upgradesSpinner = view.findViewById(R.id.spinner_upgrades);
        paymentTypeSpinner = view.findViewById(R.id.spinner_payment_type);
        subtotalTextView = view.findViewById(R.id.text_subtotal);
        taxTextView = view.findViewById(R.id.text_tax);
        totalTextView = view.findViewById(R.id.text_total);
        confirmAndPayButton = view.findViewById(R.id.button_confirm_and_pay);
        cancelSubscriptionLink = view.findViewById(R.id.text_cancel_subscription_link);

        setupSpinners();
        setupClickListeners(view);
    }

    /**
     * Extracts the original map key by stripping the appended price from the formatted spinner text.
     */
    private String getOriginalKey(String selectedItemText) {
        // Find the index of the string " ($" which marks the start of the appended price.
        int priceStart = selectedItemText.lastIndexOf(" ($");

        if (priceStart > 0) {
            // If the price marker is found, return the substring before it.
            return selectedItemText.substring(0, priceStart);
        }
        // If it's the "Select Upgrade" option (which has no price), return the text as is.
        return selectedItemText;
    }

    /**
     * Sets up the data and listeners for the three Spinner dropdowns.
     */
    private void setupSpinners() {
        // 1. Free Items Spinner
        String[] freeItems = {"None", "Feature X (Lifetime)", "Feature Y (3-months)"};
        ArrayAdapter<String> freeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, freeItems);
        freeItemsSpinner.setAdapter(freeAdapter);

        // 2. Upgrades Spinner (with Price)
        String[] upgradeOptions = upgradePrices.keySet().stream()
                .map(key -> key + (upgradePrices.get(key) > 0 ? " ($" + String.format(Locale.getDefault(), "%.2f", upgradePrices.get(key)) + ")" : ""))
                .toArray(String[]::new);

        ArrayAdapter<String> upgradeAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, upgradeOptions);
        upgradesSpinner.setAdapter(upgradeAdapter);

        // --- Set Default Selection and Calculate Price Immediately ---
        final int defaultSelectionIndex = 1;

        upgradesSpinner.post(() -> {
            upgradesSpinner.setSelection(defaultSelectionIndex);

            String selectedItemText = (String) upgradesSpinner.getItemAtPosition(defaultSelectionIndex);
            String originalKey = getOriginalKey(selectedItemText);

            if (upgradePrices.containsKey(originalKey)) {
                currentSubtotal = upgradePrices.get(originalKey);
            }
            updatePriceDisplay(currentSubtotal);
        });

        // --- Listener for subsequent selections ---
        upgradesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItemText = parent.getItemAtPosition(position).toString();
                String originalKey = getOriginalKey(selectedItemText);

                if (upgradePrices.containsKey(originalKey)) {
                    currentSubtotal = upgradePrices.get(originalKey);
                } else {
                    currentSubtotal = 0.00;
                }
                updatePriceDisplay(currentSubtotal);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // 3. Payment Type Spinner
        String[] paymentTypes = {"Visa **** 1234", "MasterCard **** 5678", "Amex **** 9012", "PayPal", "Google Pay"};
        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_dropdown_item, paymentTypes);
        paymentTypeSpinner.setAdapter(paymentAdapter);
        paymentTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPaymentMethod = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (parent.getCount() > 0) {
                    selectedPaymentMethod = parent.getItemAtPosition(0).toString();
                }
            }
        });
    }

    /**
     * Recalculates tax and total, then updates the summary TextViews and the button text.
     */
    private void updatePriceDisplay(double subtotal) {
        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;

        // Use currency formatter for consistent display
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

        subtotalTextView.setText("Subtotal: " + currencyFormat.format(subtotal));
        taxTextView.setText("Tax (" + String.format(Locale.getDefault(), "%.0f%%", TAX_RATE * 100) + "): " + currencyFormat.format(tax));
        totalTextView.setText("Total: " + currencyFormat.format(total));

        // Update the button text with the final total
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
                        // User clicked Yes, handle cancellation logic
                        Toast.makeText(getContext(), "Your Upgrade will be canceled in 10 business days.", Toast.LENGTH_LONG).show();
                        // You might want to update the UI or navigate away here
                    }
                })
                .setNegativeButton("No, Keep It", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked No, the dialog is dismissed, and they return to the payment page.
                        Toast.makeText(getContext(), "Upgrade maintained.", Toast.LENGTH_SHORT).show();
                    }
                })
                // ⭐ UPDATED ICON: Using R.drawable.logolife
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
            // Logic for processing payment
            Toast.makeText(getContext(),
                    "Processing payment of " + confirmAndPayButton.getText().toString().substring("Continue and Pay ".length())
                            + " using: " + selectedPaymentMethod,
                    Toast.LENGTH_LONG).show();
        });

        // "View Receipts" link
        TextView viewReceiptsLink = view.findViewById(R.id.text_view_receipts_link);
        if (viewReceiptsLink != null) {
            viewReceiptsLink.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Navigating to Receipts History Screen...", Toast.LENGTH_SHORT).show();
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