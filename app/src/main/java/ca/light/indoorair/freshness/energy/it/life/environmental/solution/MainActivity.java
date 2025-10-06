package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.activity.EdgeToEdge;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
public class MainActivity extends AppCompatActivity {

    FragmentManager fragmentManager;
    ActionBar actionBar;
    Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";

    // Handles the result of the permission request
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize the permission result launcher. This handles the user's response to the permission request.
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            View mainView = findViewById(R.id.main);
            if (isGranted) {
                // Permission is granted. Show a snackbar.
                Snackbar.make(mainView, "Camera permission granted.", Snackbar.LENGTH_SHORT).show();
                // You can now launch the camera or perform the related action.
            } else {
                // Permission is denied. Show a different snackbar.
                Snackbar.make(mainView, "Camera permission denied. The feature is unavailable.", Snackbar.LENGTH_LONG).show();
            }
        });

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.main, new DashBoardFragment()).commit();

        Fragment dashBoardFragment = new DashBoardFragment();
        Fragment notificationFragment = new NotificationFragment();
        Fragment settingsFragment = new SettingsFragment();

        setCurrentFragment(dashBoardFragment);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.dashboard) {
                setCurrentFragment(dashBoardFragment);
                return true;
            } else if (id == R.id.notification) {
                setCurrentFragment(notificationFragment);
                return true;
            } else if (id == R.id.settings) {
                setCurrentFragment(settingsFragment);
                return true;
            }
            return true;
        });

        // Load the saved theme preference
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Apply the loaded theme
        if (isDarkMode) {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        if (drawerLayout != null && navigationView != null) {
            toggle = new ActionBarDrawerToggle(
                    this,
                    drawerLayout,
                    toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
            );
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    setTitle(getString(R.string.home));
                } else if (id == R.id.nav_sensors) {
                    Intent intent = new Intent(MainActivity.this, SensorActivity.class);
                    startActivity(intent);
                } else if (id == R.id.nav_settings) {
                    setTitle(getString(R.string.settings));
                }
                item.setChecked(true);
                drawerLayout.closeDrawers();
                return true;
            });
        }

        // Intercept back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.logolife))
                        .setTitle(R.string.exit_application)
                        .setMessage(R.string.are_you_sure_you_want_to_exit)
                        .setPositiveButton(R.string.yes, (dialog, which) -> finish())
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        });
    }

    // Inflate the options menu from XML
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu item selections
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the navigation drawer toggle
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.themetoggle) {
            toggleTheme();
            return true;
        } else if (id == R.id.action_camera) {
            requestCameraPermission();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Method to check for and request camera permission
    private void requestCameraPermission() {
        // Check if permission is already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(R.id.main), "Camera permission is already granted.", Snackbar.LENGTH_SHORT).show();
            // You can launch the camera here if you want
        } else {
            // Permission is not granted, so request it.
            // The result is handled by the 'requestPermissionLauncher' defined in onCreate.
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // Toggles the app's theme between light and dark mode
    private void toggleTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (isDarkMode) {
            editor.putBoolean(THEME_KEY, false);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            editor.putBoolean(THEME_KEY, true);
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        editor.apply();
    }

    // Replaces the current fragment in the main container
    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment)
                .commit();
    } }
