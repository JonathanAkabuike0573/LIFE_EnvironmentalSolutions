//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashBoardFragment extends Fragment {

    // Declare the view variable
    private TextView userGreeting;

    // Declare Firebase variables
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public DashBoardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dash_board, container, false);



        List<item> items = new ArrayList<>();
        items.add(new item("Air Quality", "Good", R.drawable.air_qualityicon, true, true));
        items.add(new item("Smart Light", "Off", R.drawable.light, false, true));
        items.add(new item("Thermostat", "22°C", R.drawable.thermostaticon, true, false));
        items.add(new item("Air Conditioner", "Off", R.drawable.airconditionericon, false, true));
        items.add(new item("Wifi", "Off", R.drawable.logolife, false, true));
        items.add(new item("Smart TV", "Off", R.drawable.logolife, false, true));




        RecyclerView recycler = view.findViewById(R.id.recyclerView);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recycler.setAdapter(new MyAdapter(getContext(), items));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views and Firebase Auth
        userGreeting = view.findViewById(R.id.usergreeting); // Make sure this ID exists in fragment_dash_board.xml
        mAuth = FirebaseAuth.getInstance();

        // Load the user's information to set the greeting
        loadGreeting(new FirebaseUserDataProvider());
    }

    /**
     * This method depends on an abstraction (UserDataProvider), not a concrete class.
     * This is Method Injection.
     */
    private void loadGreeting(UserDataProvider dataProvider) {
        // Use the injected provider to fetch data.
        dataProvider.fetchUserData(new UserDataProvider.UserDataCallback() {
            @Override
            public void onDataReceived(String userName) {
                // When data is received, update the UI.
                String firstName = userName.split(" ")[0];
                setGreeting(firstName);
            }

            @Override
            public void onError(String errorMessage) {
                // If there's an error, show a generic greeting and a toast.
                setGreeting("User");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * Determines the time of day and sets the appropriate greeting message.
     * @param name The user's name to include in the greeting.
     */
    private void setGreeting(String name) {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hourOfDay >= 0 && hourOfDay < 12) {
            greeting = "Good morning";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            greeting = "Good afternoon";
        } else {
            greeting = "Good evening";
        }

        String fullGreeting = greeting + ", " + name;
        userGreeting.setText(fullGreeting);
    }
}
