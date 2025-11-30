package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.FeedBackPage;

public class DashBoardFragment extends Fragment {

    public Object dataProvider;
    // UI Elements
    private TextView userGreeting, temperatureText, comfortLevelText;
    private ImageView settingsIcon;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference presenceRef;
    private ValueEventListener presenceListener;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private MyAdapter adapter;
    private List<item> allItems;
    private List<item> visibleItems;

    public DashBoardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dash_board, container, false);

        allItems = new ArrayList<>();
        allItems.add(new item("Air Quality", "Good", R.drawable.air_qualityicon, true, false));
        allItems.add(new item("Smart Light", "Off", R.drawable.lightofficon, false, true));
        allItems.add(new item("Thermostat", "22°C", R.drawable.thermostaticon, true, false));
        allItems.add(new item("Air Conditioner", "Off", R.drawable.airconditionericon, false, true));
        allItems.add(new item("Presence Sensor", "Off", R.drawable.sensor_occupied, false, true));
        allItems.add(new item("Smart TV", "Off", R.drawable.tv_officon, false, true));

        visibleItems = new ArrayList<>();

        RecyclerView recycler = view.findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new MyAdapter(getContext(), visibleItems);
        recycler.setAdapter(adapter);
        FloatingActionButton fab = view.findViewById(R.id.fab_open_feedback);
        fab.setOnClickListener(v -> {
            // Open Feedback fragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main, new FeedBackPage())
                    .addToBackStack(null)
                    .commit();

            // Show Snackbar confirmation
            Snackbar.make(v, "Opening feedback form...", Snackbar.LENGTH_SHORT).show();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        mAuth = FirebaseAuth.getInstance();
        if (getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        settingsIcon.setOnClickListener(v -> showSettingsDialog());

        loadGreeting(new FirebaseUserDataProvider());
        initializeFirebase();
        setupPreferenceListener();
        updateVisibleItems();
        updateAirQualityStatus();
    }

    private void initializeViews(View view) {
        userGreeting = view.findViewById(R.id.usergreeting);
        temperatureText = view.findViewById(R.id.temperature_text);
        comfortLevelText = view.findViewById(R.id.weather_description); // Reusing for comfort level
        settingsIcon = view.findViewById(R.id.settings_icon);
    }

    private void showSettingsDialog() {
        String[] itemNames = new String[allItems.size()];
        boolean[] checkedItems = new boolean[allItems.size()];

        for (int i = 0; i < allItems.size(); i++) {
            itemNames[i] = allItems.get(i).getName();
            checkedItems[i] = sharedPreferences.getBoolean("item_" + i, true);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Items to Display");
        builder.setMultiChoiceItems(itemNames, checkedItems, (dialog, which, isChecked) -> {
            checkedItems[which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (int i = 0; i < checkedItems.length; i++) {
                editor.putBoolean("item_" + i, checkedItems[i]);
            }
            editor.apply();
            updateVisibleItems();
        });

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateVisibleItems() {
        visibleItems.clear();
        for (int i = 0; i < allItems.size(); i++) {
            if (sharedPreferences.getBoolean("item_" + i, true)) {
                visibleItems.add(allItems.get(i));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void initializeFirebase() {
        presenceRef = FirebaseDatabase.getInstance().getReference("room_occupancy");
        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the last entry
                    DataSnapshot lastReading = null;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        lastReading = snapshot;
                    }

                    if (lastReading != null) {
                        Double tempC = lastReading.child("current_temperature_c").getValue(Double.class);
                        Double tempF = lastReading.child("current_temperature_f").getValue(Double.class);
                        String comfortLevel = lastReading.child("comfort_level").getValue(String.class);

                        updateWeatherCard(tempC, tempF, comfortLevel);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void setupPreferenceListener() {
        preferenceChangeListener = (sharedPreferences, key) -> {
            if (key.equals("air_quality_description")) {
                updateAirQualityStatus();
            }
        };
    }

    private void updateAirQualityStatus() {
        if (sharedPreferences != null && adapter != null) {
            String airQuality = sharedPreferences.getString("air_quality_description", "Good");
            for (item i : allItems) {
                if (i.getName().equals("Air Quality")) {
                    i.setStatus(airQuality);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (presenceRef != null && presenceListener != null) {
            presenceRef.addValueEventListener(presenceListener);
        }
        if (sharedPreferences != null) {
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
            updateAirQualityStatus();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (presenceRef != null && presenceListener != null) {
            presenceRef.removeEventListener(presenceListener);
        }
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
    }

    private void updateWeatherCard(Double tempC, Double tempF, String comfortLevel) {
        if (sharedPreferences == null && getContext() != null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        }
        String unit = sharedPreferences.getString("units", "Metric (°C)");

        if (unit.equals("Imperial (°F)") && tempF != null) {
            temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°F", tempF));
        } else if (tempC != null) {
            temperatureText.setText(String.format(java.util.Locale.getDefault(), "%.0f°C", tempC));
        }

        if (comfortLevel != null) {
            comfortLevelText.setText(comfortLevel.replace("_", " "));
        }
    }

    protected void loadGreeting(UserDataProvider dataProvider) {
        dataProvider.fetchUserData(new UserDataProvider.UserDataCallback() {
            @Override
            public void onDataReceived(String userName) {
                if (userName != null && !userName.trim().isEmpty()) {
                    String firstName = userName.split(" ")[0];
                    setGreeting(firstName);
                } else {
                    setGreeting("User");
                }
            }

            @Override
            public void onError(String errorMessage) {
                setGreeting("User");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    protected Calendar getCalendarInstance() {
        return Calendar.getInstance();
    }

    String generateGreetingMessage(String name, Calendar calendar) {
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        return greeting + ", " + name;
    }

    protected void setGreeting(String name) {
        Calendar calendar = getCalendarInstance();
        String fullGreeting = generateGreetingMessage(name, calendar);
        if (userGreeting != null) {
            userGreeting.setText(fullGreeting);
        }
    }

    public void setDataProvider(FakeUserDataProvider provider) {
        this.dataProvider = provider;
    }
}
