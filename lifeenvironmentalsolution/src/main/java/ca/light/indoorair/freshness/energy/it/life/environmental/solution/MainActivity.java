package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.widget.Switch;
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
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, R.string.camera_permission_granted, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show();
                }
            });


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean isPortraitLock = sharedPreferences.getBoolean("portrait_lock", false);
        if (isPortraitLock) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        auth = FirebaseAuth.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigationView.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

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
            return true;
        });

        fragmentManager = getSupportFragmentManager();

        Fragment dashBoardFragment = new DashBoardFragment();
        Fragment settingsFragment = new SettingsFragment();
        Fragment purchasesFragment = new PurchasesFragment();
        Fragment AirQualityFragment = new AirQualityFragment();
        Fragment LightFragment = new LightFragment();
        Fragment PresenceFragment = new PresenceFragment();

        setCurrentFragment(dashBoardFragment);
        setTitle(getString(R.string.home));

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.dashboard) {
                setCurrentFragment(dashBoardFragment);
                getSupportActionBar().setTitle(getString(R.string.home));
                toolbar.setTitle(getString(R.string.home));
                return true;
            } else if (id == R.id.air_quality) {
                setCurrentFragment(new AirQualityFragment());
                getSupportActionBar().setTitle(getString(R.string.air_quality));
                toolbar.setTitle(getString(R.string.air_quality));
                return true;
            } else if (id == R.id.light) {
                setCurrentFragment(new LightFragment());
                getSupportActionBar().setTitle(getString(R.string.light));
                toolbar.setTitle(getString(R.string.light));
                return true;
            } else if (id == R.id.presence) {
                setCurrentFragment(new PresenceFragment());
                getSupportActionBar().setTitle(getString(R.string.presence));
                toolbar.setTitle(getString(R.string.presence));
                return true;
            }
            return true;
        });

        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

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

            updateNavHeader();

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    setCurrentFragment(dashBoardFragment);
                    bottomNavigationView.setSelectedItemId(R.id.dashboard);
                } else if (id == R.id.nav_settings) {
                    setCurrentFragment(settingsFragment);
                    getSupportActionBar().setTitle(getString(R.string.settings));
                    toolbar.setTitle(getString(R.string.settings));
                    toggleBottomNavigationView();
                } else if (id == R.id.nav_purchase) {
                    setCurrentFragment(purchasesFragment);
                    getSupportActionBar().setTitle(getString(R.string.purchases));
                    toolbar.setTitle(getString(R.string.purchases));
                } else if (id == R.id.nav_sign_out) {
                    signOut();
                } else if (id == R.id.nav_feedback) {
                    setCurrentFragment(new FeedBackPage());
                    toggleBottomNavigationView();
                    getSupportActionBar().setTitle(getString(R.string.feedback));
                    toolbar.setTitle(getString(R.string.feedback));
                } else if (id == R.id.nav_about) {
                    setTitle(getString(R.string.about_us));
                    setCurrentFragment(new AboutUsFragment());
                    toggleBottomNavigationView();
                }
                drawerLayout.closeDrawers();
                return true;
            });
        }

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

    private void updateNavHeader() {
        if (navigationView == null) return;

        View headerView = navigationView.getHeaderView(0);

        TextView navUserName = headerView.findViewById(R.id.navheaderusername);
        TextView navUserEmail = headerView.findViewById(R.id.navheaderemail);

        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null) navUserEmail.setText(email);

            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

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
            navUserName.setText("Guest User");
            navUserEmail.setText("");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean(THEME_KEY, false);

        int color = isDarkMode ?
                ContextCompat.getColor(this, android.R.color.white) :
                ContextCompat.getColor(this, android.R.color.black);

        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            applyMenuItemColor(menuItem, color);

            if (menuItem.hasSubMenu()) {
                for (int j = 0; j < menuItem.getSubMenu().size(); j++) {
                    applyMenuItemColor(menuItem.getSubMenu().getItem(j), color);
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

    // ---------------------------------------------------------
    //  ⭐ UPDATED THEME BUTTON HANDLING
    // ---------------------------------------------------------
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.action_notification) {
            askNotificationPermission();
            return true;

        } else if (itemId == R.id.action_togglemode) {
            toggleTheme();     // flip dark/light mode
            recreate();        // refresh UI
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // ---------------------------------------------------------

    private void toggleBottomNavigationView() {
        if (bottomNavigationView.getVisibility() == View.VISIBLE) {
            bottomNavigationView.animate().translationY(bottomNavigationView.getHeight())
                    .withEndAction(() -> bottomNavigationView.setVisibility(View.GONE));

        } else {
            bottomNavigationView.setVisibility(View.VISIBLE);
            bottomNavigationView.animate().translationY(1);
        }
    }

    private void askCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission already granted.", Toast.LENGTH_SHORT).show();
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
        auth.signOut();

        Identity.getSignInClient(this).signOut().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void askNotificationPermission() {
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
