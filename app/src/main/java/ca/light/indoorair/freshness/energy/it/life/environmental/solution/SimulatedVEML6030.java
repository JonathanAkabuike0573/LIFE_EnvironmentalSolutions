package ca.light.indoorair.freshness.energy.it.life.environmental.solution;

import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Simulated Vishay VEML6030 ambient light sensor.
 * - Supports gain & integration time settings (coarse simulation).
 * - Produces periodic lux samples with drift+noise.
 * - Optional "scenes" to quickly set realistic ranges.
 *
 * NOTE: This is a simulator for demos/tests—values are plausible, not calibrated.
 */
public class SimulatedVEML6030 {

    // ---- Public API ----------------------------------------------------------

    public enum Gain {
        GAIN_1_8(0.125f), GAIN_1_4(0.25f), GAIN_1(1f), GAIN_2(2f);
        final float factor;
        Gain(float f) { this.factor = f; }
    }

    public enum IntegrationTime {
        IT_25MS(25), IT_50MS(50), IT_100MS(100), IT_200MS(200),
        IT_400MS(400), IT_800MS(800);
        final int ms;
        IntegrationTime(int ms) { this.ms = ms; }
    }

    public enum Scene {
        NIGHT(0, 5),
        INDOOR_DIM(5, 60),
        INDOOR_BRIGHT(60, 500),
        OFFICE(200, 800),
        OUTDOOR_SHADE(1000, 8000),
        OUTDOOR_DAY(8000, 30000),
        OUTDOOR_SUNNY(30000, 100000);

        final int minLux;
        final int maxLux;
        Scene(int minLux, int maxLux) { this.minLux = minLux; this.maxLux = maxLux; }
    }

    /** Sample callback (invoked on the main thread). */
    public interface OnSampleListener {
        /** @param lux simulated lux (post-processed with gain/IT & saturation) */
        void onSample(double lux, int rawCounts, long timestampMillis);
    }

    public SimulatedVEML6030() {
        mainHandler = new Handler(Looper.getMainLooper());
        scheduler = new ScheduledThreadPoolExecutor(1);
        random = new Random();
        setScene(Scene.INDOOR_BRIGHT);
        setGain(Gain.GAIN_1);
        setIntegrationTime(IntegrationTime.IT_100MS);
    }

    public void setOnSampleListener(OnSampleListener l) { this.listener = l; }

    public void setGain(Gain g) { this.gain = g; }

    public void setIntegrationTime(IntegrationTime it) { this.it = it; }

    /** Quickly configure min/max lux generation. */
    public void setScene(Scene scene) {
        this.minLux = scene.minLux;
        this.maxLux = scene.maxLux;
        // reset baseline around the middle of the range
        this.trueLux = (minLux + maxLux) / 2.0;
    }

    /** Manually constrain the simulated range. */
    public void setLuxRange(int minLux, int maxLux) {
        if (minLux < 0) minLux = 0;
        if (maxLux < minLux + 1) maxLux = minLux + 1;
        this.minLux = minLux;
        this.maxLux = maxLux;
        this.trueLux = Math.max(minLux, Math.min(trueLux, maxLux));
    }

    /** Start periodic sampling (emits on the main thread). */
    public void start() {
        if (running) return;
        running = true;
        scheduleNext();
    }

    /** Stop periodic sampling. */
    public void stop() {
        running = false;
        scheduler.getQueue().clear();
    }

    /** One-shot read (no scheduling). */
    public Sample readOnce() {
        updateTrueLux();
        return computeReading();
    }

    /** Holds one reading (lux + raw 16-bit count). */
    public static class Sample {
        public final double lux;
        public final int rawCounts;
        public final long timestampMillis;
        public Sample(double lux, int rawCounts, long ts) {
            this.lux = lux; this.rawCounts = rawCounts; this.timestampMillis = ts;
        }
        @Override public String toString() {
            return String.format(Locale.US, "%.1f lux (%d cnts)", lux, rawCounts);
        }
    }

    // ---- Internals -----------------------------------------------------------

    private static final int ADC_MAX = 65535;

    private final Handler mainHandler;
    private final ScheduledThreadPoolExecutor scheduler;
    private final Random random;

    private OnSampleListener listener;
    private boolean running = false;

    // simulation state
    private int minLux = 5, maxLux = 500;
    private double trueLux = 100.0;   // slowly drifts within [minLux, maxLux]
    private double driftPerTick = 0.02; // small trend change per tick

    private Gain gain = Gain.GAIN_1;
    private IntegrationTime it = IntegrationTime.IT_100MS;

    private long lastTickNs = 0L;

    private void scheduleNext() {
        if (!running) return;
        scheduler.schedule(this::tick, it.ms, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (!running) return;

        updateTrueLux();
        Sample s = computeReading();

        if (listener != null) {
            mainHandler.post(() -> listener.onSample(s.lux, s.rawCounts, s.timestampMillis));
        }
        scheduleNext();
    }

    private void updateTrueLux() {
        // Random walk (bounded): add small noise + occasional drift nudges
        double noise = random.nextGaussian() * (maxLux - minLux) * 0.005; // 0.5% of range
        double drift = (random.nextDouble() - 0.5) * driftPerTick * (maxLux - minLux);

        trueLux += noise + drift;

        // Occasionally steer toward center to avoid hitting bounds too long
        if (random.nextDouble() < 0.05) {
            double center = (minLux + maxLux) / 2.0;
            trueLux += (center - trueLux) * 0.02;
        }

        if (trueLux < minLux) trueLux = minLux + random.nextDouble() * 5;
        if (trueLux > maxLux) trueLux = maxLux - random.nextDouble() * 5;
    }

    /**
     * Compute counts and displayed lux roughly following how gain/IT affect sensitivity.
     * We emulate an LSB (lux per count) that depends on IT and gain:
     *  - Longer IT => more sensitivity (smaller LSB)
     *  - Higher gain => more sensitivity (smaller LSB)
     *
     * This is NOT the datasheet formula—it's a plausible model for demos.
     */
    private Sample computeReading() {
        long now = System.currentTimeMillis();

        // Base LSB (lux per count) for IT=100ms, gain=1 (arbitrary but stable)
        double baseLsb = 0.01; // 0.01 lux/count

        // Integration time scaling: halving IT doubles LSB (less sensitive), etc.
        double itScale = 100.0 / it.ms;      // e.g., 100ms -> 1.0, 200ms -> 0.5
        // Gain scaling: higher gain reduces LSB (more sensitive)
        double gainScale = 1.0 / gain.factor; // e.g., gain 2 -> 0.5

        double lsb = baseLsb * itScale * gainScale;

        // Convert lux to raw counts, saturate at 16-bit
        int counts = (int)Math.round(trueLux / lsb);
        if (counts > ADC_MAX) counts = ADC_MAX;
        if (counts < 0) counts = 0;

        // Convert back to what the app would display
        double displayedLux = counts * lsb;

        return new Sample(displayedLux, counts, now);
    }
}