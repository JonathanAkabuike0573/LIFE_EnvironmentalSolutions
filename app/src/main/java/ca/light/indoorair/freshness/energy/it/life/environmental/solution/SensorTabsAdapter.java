package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SensorTabsAdapter extends FragmentStateAdapter {

    public SensorTabsAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance based on position
        switch (position) {
            case 0:
                return new AirQualityFragment();
            case 1:
                return new LightFragment();
            case 2:
                return new PresenceFragment();
            default:
                // Should not happen, but good practice to have a default
                return new AirQualityFragment();
        }
    }

    @Override
    public int getItemCount() {
        // The total number of tabs
        return 3;
    }
}

