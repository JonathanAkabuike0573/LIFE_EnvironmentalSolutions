package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
public class PurchasesFragment extends Fragment {

    public PurchasesFragment() {

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

        // new "Confirm and Pay" button
        Button confirmAndPayButton = view.findViewById(R.id.button_confirm_and_pay);

        // Set up a click listener for the main payment action
        confirmAndPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(getContext(), "Processing Payment using selected method...", Toast.LENGTH_LONG).show();
            }
        });

        //  Add functionality for the "View Receipts" link
        TextView viewReceiptsLink = view.findViewById(R.id.text_view_receipts_link);
        if (viewReceiptsLink != null) {
            viewReceiptsLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(getContext(), "Navigating to Receipts History Screen...", Toast.LENGTH_SHORT).show();
                }
            });
        } } }