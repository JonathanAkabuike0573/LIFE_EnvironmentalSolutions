package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private GestureDetector gestureDetector;
    private BottomNavigationView bottomNavigationView;


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
                    Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_SHORT).show();
                    // Intent to open camera can be placed here.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Toggling navigation bar visibility will change insets, so we need to handle it.
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // We apply padding for status bar and navigation bar, but for the bottom part
            // we let the bottom navigation view handle its own padding.
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigationView.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        // Setup gesture detector to listen for screen taps
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleBottomNavigationView();
                return true;
            }
        });

        View mainContent = findViewById(R.id.main);
        mainContent.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true; // Consume the event
        });
        fragmentManager = getSupportFragmentManager();

        Fragment dashBoardFragment = new DashBoardFragment();
        Fragment sensorFragment = new SensorFragment();
        Fragment settingsFragment = new SettingsFragment();
        Fragment purchasesFragment = new PurchasesFragment();

        setCurrentFragment(dashBoardFragment);
        setTitle(getString(R.string.home));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.dashboard) {
                setCurrentFragment(dashBoardFragment);
                setTitle(getString(R.string.home));
                return true;
            } else if (id == R.id.notification) {
                setCurrentFragment(sensorFragment);
                setTitle(getString(R.string.sensors));
                return true;
            } else if (id == R.id.purchases) {
                setCurrentFragment(purchasesFragment);
                setTitle(getString(R.string.purchases));
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

            //  Load user's name and email into the navigation header
            updateNavHeader();

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    setCurrentFragment(dashBoardFragment);
                    setTitle(getString(R.string.home));
                    bottomNavigationView.setSelectedItemId(R.id.dashboard);
                } else if (id == R.id.nav_sensors) {
                    setCurrentFragment(sensorFragment);
                    setTitle(getString(R.string.sensors));
                    bottomNavigationView.setSelectedItemId(R.id.notification);
                } else if (id == R.id.nav_settings) {
                    setCurrentFragment(settingsFragment);
                    setTitle(getString(R.string.settings));
                } else if (id == R.id.nav_sign_out) {
                    signOut();
                } else if (id == R.id.nav_feedback) {
                    setCurrentFragment(new FeedBackPage());
                }
                // --- ADD THIS NEW BLOCK ---
                else if (id == R.id.nav_about) {
                    setTitle(getString(R.string.about_us));
                    setCurrentFragment(new AboutUsFragment());
                }
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

    /**
     * Finds the current user and updates the navigation header with their name and email.
     */
    private void updateNavHeader() {
        // Ensure navigationView is not null
        if (navigationView == null) return;

        View headerView = navigationView.getHeaderView(0);

        // Use your specific IDs from nav_header.xml
        TextView navUserName = headerView.findViewById(R.id.navheaderusername);
        TextView navUserEmail = headerView.findViewById(R.id.navheaderemail);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Set the email directly from the FirebaseUser object
            String email = currentUser.getEmail();
            if (email != null) {
                navUserEmail.setText(email);
            }

            // To get the user's name, read it from the Realtime Database
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        HelperClass userProfile = snapshot.getValue(HelperClass.class);
                        if (userProfile != null) {
                            String name = userProfile.getName();
                            navUserName.setText(name);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to load user name.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Handle case where there is no signed-in user
            navUserName.setText("Guest User");
            navUserEmail.setText("");
        }
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

                    applyMenuItemColor(subMenuItem, color);
                }
                if (menuItem.getItemId() == R.id.notification) {

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
        int itemId = item.getItemId();
        if (itemId == R.id.themetoggle) {
            toggleTheme();
            return true;
        } else if (itemId == R.id.action_notification) {
            askNotificationPermission();
            return true;
        } else if (itemId == R.id.action_camera) {
            askCameraPermission();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleBottomNavigationView() {
        if (bottomNavigationView.getVisibility() == View.VISIBLE) {
            bottomNavigationView.animate().translationY(bottomNavigationView.getHeight()).withEndAction(() -> bottomNavigationView.setVisibility(View.GONE));

        } else {
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.animate().translationY(1);
        }
    }

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission already granted.", Toast.LENGTH_SHORT).show();
            // You can open camera here
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
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

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.main, fragment).commit();
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
        } else {
            Toast.makeText(this, "Notification permission not required on this Android version.", Toast.LENGTH_SHORT).show();
        }
    }
}
