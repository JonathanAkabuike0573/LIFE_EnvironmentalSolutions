package ca.light.indoorair.freshness.energy.it.life.environmental.solution;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashWorks extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install SplashScreen API
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Keep the splash for 3 seconds
        setContentView(R.layout.splashworksdarkmode);

        // Start MainActivity after delay


        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            Intent intent = new Intent(SplashWorks.this, MainActivity.class);
            startActivity(intent);
            finish();

        }, SPLASH_DELAY);

    } }

