package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.Menu;
import android.view.MenuItem;
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

public class MainActivity extends AppCompatActivity {

    private FragmentManager fragmentManager;
    private FirebaseAuth auth;
    private ActionBar actionBar;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private BottomNavigationView bottomNavigationView;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String THEME_KEY = "ThemeKey";

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupWindowInsets();
        setupToolbar();

        auth = FirebaseAuth.getInstance();
        fragmentManager = getSupportFragmentManager();

        setupTheme();
        setupNavigation();

        if (savedInstanceState == null) {
            setCurrentFragment(new DashBoardFragment());
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
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(false);
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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(navigationView)) {
                    drawerLayout.closeDrawers();
                } else {
                    showExitDialog();
                }
            }
        });
    }

    private void handleDrawerNavigation(int itemId) {
        if (itemId == R.id.nav_home) {
            setCurrentFragment(new DashBoardFragment());
            getSupportActionBar().setTitle(R.string.dashboard);
            bottomNavigationView.setSelectedItemId(R.id.dashboard);
        } else if (itemId == R.id.nav_sign_out) {
            signOut();
        } else {
            clearBottomNavSelection();
            if (itemId == R.id.nav_profile) {
                setCurrentFragment(new AccountFragment());
                getSupportActionBar().setTitle("Profile");
            } else if (itemId == R.id.nav_settings) {
                setCurrentFragment(new SettingsFragment());
                getSupportActionBar().setTitle(getString(R.string.settings));
            } else if (itemId == R.id.nav_purchase) {
                setCurrentFragment(new PurchasesFragment());
                getSupportActionBar().setTitle(getString(R.string.purchases));
            } else if (itemId == R.id.nav_feedback) {
                setCurrentFragment(new FeedBackPage());
                getSupportActionBar().setTitle(getString(R.string.feedback));
            } else if (itemId == R.id.nav_about) {
                setCurrentFragment(new AboutUsFragment());
                getSupportActionBar().setTitle(getString(R.string.about_us));
            }
        }
    }

    private void handleBottomNavigation(int itemId) {
        bottomNavigationView.getMenu().findItem(itemId).setChecked(true);
        if (itemId == R.id.dashboard) {
            setCurrentFragment(new DashBoardFragment());
            getSupportActionBar().setTitle(R.string.dashboard);
        } else if (itemId == R.id.air_quality) {
            setCurrentFragment(new AirQualityFragment());
            getSupportActionBar().setTitle(R.string.air_quality);
        } else if (itemId == R.id.light) {
            setCurrentFragment(new LightFragment());
            getSupportActionBar().setTitle(R.string.light);
        } else if (itemId == R.id.presence) {
            setCurrentFragment(new PresenceFragment());
            getSupportActionBar().setTitle(R.string.presence);
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

    private void clearBottomNavSelection() {
        bottomNavigationView.getMenu().setGroupCheckable(0, true, false);
        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }
        bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
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
        int color = ContextCompat.getColor(this, isDarkMode ? android.R.color.white : android.R.color.black);

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        int itemId = item.getItemId();
        if (itemId == R.id.action_notification) {
            askNotificationPermission();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCurrentFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main, fragment);
        transaction.commit();
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Notification permission not required on this Android version.", Toast.LENGTH_SHORT).show();
        }
    }
}
