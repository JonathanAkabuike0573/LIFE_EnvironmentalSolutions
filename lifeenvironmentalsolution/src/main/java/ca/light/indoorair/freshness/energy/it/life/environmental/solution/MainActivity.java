package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.auth.HelperClass;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.AboutUsFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.AccountFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.AirQualityFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.EnergyFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.FeedBackPage;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.LightFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.NotificationFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.PresenceFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.PurchasesFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.SettingsFragment;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui.HelpFragment;


// Implement OnBackStackChangedListener to react to navigation changes
public class MainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private FragmentManager fragmentManager;
    private FirebaseAuth auth;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private BottomNavigationView bottomNavigationView;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWindowInsets();
        setupToolbar();

        auth = FirebaseAuth.getInstance();
        fragmentManager = getSupportFragmentManager();

        fragmentManager.addOnBackStackChangedListener(this);

        setupTheme();
        setupNavigation();

        if (savedInstanceState == null) {
            // Use new setCurrentFragment method
            setCurrentFragment(new DashBoardFragment(), false);
            getSupportActionBar().setTitle(getString(R.string.dashboard));
        }
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            findViewById(R.id.bottom_navigation).setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    private void setupTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isPortraitLock = sharedPreferences.getBoolean("portrait_lock", false);
        setRequestedOrientation(isPortraitLock ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupNavigation() {
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            handleDrawerNavigation(item.getItemId());
            drawerLayout.closeDrawers();
            return true;
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            handleBottomNavigation(item.getItemId());
            return true;
        });

        updateNavHeader();

        setupBackButton();
    }



    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                } else if (fragmentManager.getBackStackEntryCount() > 0) {

                    fragmentManager.popBackStack();
                } else {

                    showExitDialog();
                }
            }
        });
    }

    private void handleDrawerNavigation(int itemId) {

        bottomNavigationView.setVisibility(View.GONE);

        if (itemId == R.id.nav_home) {

            bottomNavigationView.setVisibility(View.VISIBLE);
            clearBackStack();
            setCurrentFragment(new DashBoardFragment(), false);
            getSupportActionBar().setTitle(R.string.dashboard);
            bottomNavigationView.setSelectedItemId(R.id.dashboard);
        } else if (itemId == R.id.nav_sign_out) {
            signOut();
        } else {

            if (itemId == R.id.nav_profile) {
                setCurrentFragment(new AccountFragment(), true);
                getSupportActionBar().setTitle("Profile");
            } else if (itemId == R.id.nav_settings) {
                setCurrentFragment(new SettingsFragment(), true);
                getSupportActionBar().setTitle(getString(R.string.settings));
            } else if (itemId == R.id.nav_purchase) {
                setCurrentFragment(new PurchasesFragment(), true);
                getSupportActionBar().setTitle(getString(R.string.purchases));
            } else if (itemId == R.id.nav_feedback) {
                setCurrentFragment(new FeedBackPage(), true);
                getSupportActionBar().setTitle(getString(R.string.feedback));
            }
        }
    }

    private void handleBottomNavigation(int itemId) {

        bottomNavigationView.setVisibility(View.VISIBLE);


        clearBackStack();


        if (itemId == R.id.dashboard) {
            setCurrentFragment(new DashBoardFragment(), false);
            getSupportActionBar().setTitle(R.string.dashboard);
        } else if (itemId == R.id.air_quality) {
            setCurrentFragment(new AirQualityFragment(), false);
            getSupportActionBar().setTitle(R.string.air_quality);
        } else if (itemId == R.id.bi_directional) {
            setCurrentFragment(new EnergyFragment(), false);
            getSupportActionBar().setTitle("Energy");
        } else if (itemId == R.id.light) {
            setCurrentFragment(new LightFragment(), false);
            getSupportActionBar().setTitle(R.string.light);
        } else if (itemId == R.id.presence) {
            setCurrentFragment(new PresenceFragment(), false);
            getSupportActionBar().setTitle(R.string.presence);
        }
    }


    private void clearBackStack() {
        if (fragmentManager.getBackStackEntryCount() > 0) {
            FragmentManager.BackStackEntry entry = fragmentManager.getBackStackEntryAt(0);
            fragmentManager.popBackStack(entry.getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }


    @Override
    public void onBackStackChanged() {
        boolean hasBackStack = fragmentManager.getBackStackEntryCount() > 0;

        // Show back arrow if there's a back stack, otherwise show hamburger
        getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackStack);

        if (hasBackStack) {
            // If in a detail screen (like Settings), hide bottom nav and handle back press
            bottomNavigationView.setVisibility(View.GONE);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        } else {
            // If at a main screen, show bottom nav and set up the drawer toggle
            bottomNavigationView.setVisibility(View.VISIBLE);
            toggle.syncState();
            toolbar.setNavigationOnClickListener(v -> drawerLayout.open());
            // Sync bottom nav with current fragment
            syncBottomNavSelection();
        }
    }


    public void navigateToDashboardItem(String itemName, String roomName) {

        clearBackStack();

        Fragment fragment = null;
        Bundle args = new Bundle();
        args.putString("SELECTED_ROOM_KEY", roomName);

        switch (itemName) {
            case "Air Quality":
                fragment = new AirQualityFragment();
                bottomNavigationView.setSelectedItemId(R.id.air_quality);
                getSupportActionBar().setTitle(R.string.air_quality);
                break;
            case "Smart Light":
                fragment = new LightFragment();
                bottomNavigationView.setSelectedItemId(R.id.light);
                getSupportActionBar().setTitle(R.string.light);
                break;
            case "Presence Sensor":
                fragment = new PresenceFragment();
                bottomNavigationView.setSelectedItemId(R.id.presence);
                getSupportActionBar().setTitle(R.string.presence);
                break;
            case "Thermostat":
                fragment = new EnergyFragment();
                bottomNavigationView.setSelectedItemId(R.id.bi_directional);
                getSupportActionBar().setTitle("Energy");
                break;
        }

        if (fragment != null) {
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, fragment)
                    .commit();
        }
    }



    public void syncBottomNavSelection() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.main);
        if (currentFragment instanceof DashBoardFragment) {
            bottomNavigationView.setSelectedItemId(R.id.dashboard);
        } else if (currentFragment instanceof AirQualityFragment) {
            bottomNavigationView.setSelectedItemId(R.id.air_quality);
        } else if (currentFragment instanceof EnergyFragment) {
            bottomNavigationView.setSelectedItemId(R.id.bi_directional);
        } else if (currentFragment instanceof LightFragment) {
            bottomNavigationView.setSelectedItemId(R.id.light);
        } else if (currentFragment instanceof PresenceFragment) {
            bottomNavigationView.setSelectedItemId(R.id.presence);
        }
    }




    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.logolife)
                .setTitle(R.string.exit_application)
                .setMessage(R.string.are_you_sure_you_want_to_exit)
                .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navheaderusername);
        TextView navUserEmail = headerView.findViewById(R.id.navheaderemail);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            navUserEmail.setText(currentUser.getEmail());
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        HelperClass userProfile = snapshot.getValue(HelperClass.class);
                        if (userProfile != null) {
                            navUserName.setText(userProfile.getName());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to load user name.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            navUserName.setText(R.string.guest_user);
            navUserEmail.setText("");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        int color = ContextCompat.getColor(this, isDarkMode ? android.R.color.white : R.color.black);

        // The main menu text should be black when not in dark mode.
        for (int i = 0; i < menu.size(); i++) {
            applyMenuItemColor(menu.getItem(i), color);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void applyMenuItemColor(MenuItem menuItem, int color) {
        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
        SpannableString spannableString = new SpannableString(menuItem.getTitle());
        spannableString.setSpan(new ForegroundColorSpan(color), 0, spannableString.length(), 0);
        menuItem.setTitle(spannableString);

        if (menuItem.hasSubMenu()) {
            for (int j = 0; j < menuItem.getSubMenu().size(); j++) {
                applyMenuItemColor(menuItem.getSubMenu().getItem(j), color);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.action_notification) {
            setCurrentFragment(new NotificationFragment(), true);
            getSupportActionBar().setTitle(getString(R.string.notification));
            return true;

        } else if (itemId == R.id.action_aboutus) {
            setCurrentFragment(new AboutUsFragment(), true);
            getSupportActionBar().setTitle(getString(R.string.about_us));
            return true;

        } else if (itemId == R.id.action_togglemode) {
            // your existing theme toggle code here
            // ...
            return true;

        } else if (itemId == R.id.action_help) {
            // 🔹 NEW: open Help fragment
            setCurrentFragment(new HelpFragment(), true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(getString(R.string.help));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void setCurrentFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    private void signOut() {
        auth.signOut();
        Identity.getSignInClient(this).signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Sign out failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}