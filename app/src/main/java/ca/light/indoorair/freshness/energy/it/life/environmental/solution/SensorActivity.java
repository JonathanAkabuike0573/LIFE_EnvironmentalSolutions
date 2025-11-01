package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;

public class SensorActivity extends AppCompatActivity {

    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.sensor_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle(R.string.sensor_activity);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(SensorActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }


        });

        // Find views
        TabLayout tabLayout = findViewById(R.id.sensor_tab_layout);
        ViewPager2 viewPager = findViewById(R.id.sensor_view_pager);

        // Create and set the adapter
        SensorTabsAdapter adapter = new SensorTabsAdapter(this);
        viewPager.setAdapter(adapter);

        // Link the TabLayout and ViewPager2 and set tab names
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText(R.string.air_quality);
                        tab.setIcon(R.drawable.notification_foreground); // Optional icon
                    } else if (position == 1) {
                        tab.setText(R.string.light);
                        tab.setIcon(R.drawable.notification_foreground); // Optional icon
                    } else if (position == 2) {
                        tab.setText(R.string.presence);
                        tab.setIcon(R.drawable.notification_foreground); // Optional icon
                    }
                }
        ).attach();
    }
}
