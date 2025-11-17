package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Note: Ensure your build.gradle file includes androidx.appcompat or
// androidx.fragment dependencies.

/**
 * A simple {@link Fragment} subclass for displaying the About Us screen.
 * This fragment inflates the layout and no longer sets the dynamic app version text.
 */
public class AboutUsFragment extends Fragment {

    // Default public constructor is required for Fragments
    public AboutUsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about_us, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    } }