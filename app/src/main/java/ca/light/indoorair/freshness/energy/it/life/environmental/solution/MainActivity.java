//Mohamed Ali  N01440760, Jonathan Akabuike N01510573, Kieran Sharma N01548225, Farhan Habibza N01610299
//CENG-322-OCC,  Software Project
package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.Manifest;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    FragmentManager fragmentManager;
    private FirebaseAuth auth;
    ActionBar actionBar;
    Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";

    // Declare the launcher at the top of your Activity/Fragment:
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. You can now open the camera.
                    Toast.makeText(this, R.string.camera_permission_granted , Toast.LENGTH_SHORT).show();
                    // Intent to open camera can be placed here.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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
                } else if (id == R.id.nav_sign_out) {
                    signOut();
                }
                item.setChecked(true);
                drawerLayout.closeDrawers();
                return true;
            });
        }

        // Intercept back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.logolife)) // Using a system alert icon
                        .setTitle(R.string.exit_application)
                        .setMessage(R.string.are_you_sure_you_want_to_exit)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish(); // Exit the application
                            }
                        })
                        .setNegativeButton(R.string.no, null) // Do nothing, stay on the app
                        .show();
            }
        });
    }

    // Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Load the saved theme preference
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);

        // Define the color based on the current theme
        int color;
        if (isDarkMode) {
            color = ContextCompat.getColor(this, android.R.color.white);
        } else {
            color = ContextCompat.getColor(this, android.R.color.black);
        }

        // Iterate through all menu items to apply the color
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            applyMenuItemColor(menuItem, color);

            if (menuItem.hasSubMenu()) {
                for (int j = 0; j < menuItem.getSubMenu().size(); j++) {
                    MenuItem subMenuItem = menuItem.getSubMenu().getItem(j);
                    // Note: You can use a different color for sub-menu items if needed
                    applyMenuItemColor(subMenuItem, color);
                }
                if (menuItem.getItemId() == R.id.notification) {
                    // This seems to be the parent of the notification settings,
                    // let's handle the submenu item click in onOptionsItemSelected instead.
                    // This is just a placeholder to show where you might add logic if needed.
                }
            }
        }
        return true;
    }

    private void applyMenuItemColor(MenuItem menuItem, int color) {
        Drawable icon = menuItem.getIcon();
        if (icon != null) {
            icon.mutate().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
        SpannableString spannableString = new SpannableString(menuItem.getTitle());
        spannableString.setSpan(new ForegroundColorSpan(color), 0, spannableString.length(), 0);
        menuItem.setTitle(spannableString);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == R.id.themetoggle) {
            toggleTheme();
            return true;
        } else if (item.getItemId() == R.id.action_notification) { // Assuming R.id.notification_settings is your submenu item ID
            askNotificationPermission();
            return true;
        } else if (item.getItemId() == R.id.action_camera) {
            askCameraPermission();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isDarkMode) {
            editor.putBoolean(THEME_KEY, false);

            editor.apply();
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            editor.putBoolean(THEME_KEY, true);
            editor.apply();
            getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }


    }

    private void setCurrentFragment(Fragment homefragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.main, homefragment).commit();
    }

    private void signOut() {
        // Firebase sign out
        auth.signOut();

        // When a user signs out, clear the current user credential state from all credential providers.
        Identity.getSignInClient(this).signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // After successful sign out, navigate to the LoginActivity
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish(); // Finish MainActivity so user can't go back
            }
        });
    }

    private void askNotificationPermission() {
        // This is only necessary for API level 33 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.camera_permission_already_granted , Toast.LENGTH_SHORT).show();
            // Intent to open camera can be placed here.
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
}
