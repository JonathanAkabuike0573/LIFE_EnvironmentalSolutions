//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    // Define SharedPreferences constants
    public static final String PREFS_NAME = "MyPrefsFile";

    private SharedPreferences sharedPreferences;



    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

    return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);



        // TODO: Add setup for other settings like Dark Mode or Notifications here
    }
}
