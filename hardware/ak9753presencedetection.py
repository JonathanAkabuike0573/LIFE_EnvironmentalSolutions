#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import time
from datetime import datetime
import statistics

# Optional: Sense HAT Import
try:
    from sense_hat import SenseHat
    sense = SenseHat()
    sense.clear()
    print("Sense HAT initialized successfully")
except ImportError:
    print("Sense HAT not available - using AK9753 temperature")
    sense = None

try:
    import smbus
    print("Using smbus library")
except ImportError:
    print("smbus not available")
    exit(1)

# AK9753 I2C Address
AK9753_ADDRESS = 0x64

# ======== AK9753 DRIVER (matching working example) ========

class AK9753:
    AK975X_ST1    = 0x05
    AK975X_IR1    = 0x06
    AK975X_IR2    = 0x08
    AK975X_IR3    = 0x0A
    AK975X_IR4    = 0x0C
    AK975X_ST2    = 0x10
    AK975X_ECNTL1 = 0x1C
    AK975X_CNTL2  = 0x19

    def __init__(self, bus_num=1, address=AK9753_ADDRESS):
        self.bus = smbus.SMBus(bus_num)
        self.address = address

    def _write_register(self, register, value):
        self.bus.write_byte_data(self.address, register, value)

    def _read_register(self, register, length=1):
        return self.bus.read_i2c_block_data(self.address, register, length)

    def soft_reset(self):
        self._write_register(0x1D, 0x01)
        time.sleep(0.1)

    def begin(self):
        try:
            # Standby
            self._write_register(self.AK975X_ECNTL1, 0x00)
            time.sleep(0.1)
            # Continuous Mode 0, EFC=100 (0.6 Hz) -> 0x0C
            self._write_register(self.AK975X_ECNTL1, 0x0C)
            time.sleep(0.1)
            return True
        except Exception as e:
            print("Begin/config error:", e)
            return False

    def check_data_ready(self):
        status = self._read_register(self.AK975X_ST1)[0]
        return (status & 0x01) != 0

    def _read_ir16(self, reg):
        raw = self._read_register(reg, 2)
        return int.from_bytes(raw, byteorder="little", signed=True)

    def read_ir_all(self):
        ir1 = self._read_ir16(self.AK975X_IR1)
        ir2 = self._read_ir16(self.AK975X_IR2)
        ir3 = self._read_ir16(self.AK975X_IR3)
        ir4 = self._read_ir16(self.AK975X_IR4)
        self._read_register(self.AK975X_ST2)  # clear DRDY
        return ir1, ir2, ir3, ir4

    @property
    def ir1(self):
        return self._read_ir16(self.AK975X_IR1)

    @property
    def ir2(self):
        return self._read_ir16(self.AK975X_IR2)

    @property
    def ir3(self):
        return self._read_ir16(self.AK975X_IR3)

    @property
    def ir4(self):
        return self._read_ir16(self.AK975X_IR4)

    @property
    def temperature(self):
        # If you want AK9753 internal temp, add correct TMP register here.
        return 0

# ======== Support functions ========

def get_temperature(sensor):
    if sense:
        return round(sense.get_temperature(), 1)
    else:
        return sensor.temperature

def celsius_to_fahrenheit(celsius):
    return round((celsius * 9/5) + 32, 1)

def get_indoor_comfort_level(temperature_c):
    if temperature_c < 16:
        return "TOO_COLD"
    elif temperature_c < 19:
        return "SLIGHTLY_COOL"
    elif temperature_c <= 26:
        return "COMFORTABLE"
    elif temperature_c <= 29:
        return "SLIGHTLY_WARM"
    else:
        return "TOO_HOT"

class MotionFeatureState:
    def __init__(self, ea=0.05):
        self.average_value = None
        self.fa2 = 0
        self.fa2_deriv = 0
        self.fa2_deriv_last = 0
        self.ea = ea
    def update(self, sample):
        if self.average_value is None:
            self.average_value = sample
            self.fa2 = 0
            self.fa2_deriv = 0
            self.fa2_deriv_last = 0
            return self.fa2_deriv
        self.fa2_deriv_last = self.fa2_deriv
        self.fa2_deriv = int(self.average_value - sample - self.fa2)
        self.fa2 = int(self.average_value - sample)
        self.average_value = self.ea * sample + (1 - self.ea) * self.average_value
        return self.fa2_deriv

def detect_presence(ir1, ir2, ir3, ir4, cfg, state: MotionFeatureState):
    ir_total = abs(ir1) + abs(ir2) + abs(ir3) + abs(ir4)
    deriv = state.update(ir3)
    motion_total = ir_total > cfg["total_threshold"]
    motion_deriv = abs(deriv) > cfg["derivative_threshold"]
    detected = motion_total or motion_deriv
    return detected, ir_total, deriv

def calibrate_sensor(sensor, calibration_time=15):
    print("\n" + "=" * 60)
    print("ADVANCED CALIBRATION MODE")
    print("=" * 60)
    print("1) First half: keep the area in front of the sensor EMPTY / STILL.")
    print("2) Second half: move your hand or body clearly in front of the sensor.")
    print("Total calibration time: ~%d seconds" % calibration_time)
    print("-" * 60)

    start_time = time.time()
    half_time = calibration_time / 2.0

    ambient_totals = []
    ambient_ir3 = []
    active_totals = []
    active_ir3 = []

    feature_state = MotionFeatureState(ea=0.05)
    last_print = 0

    while True:
        now = time.time()
        elapsed = now - start_time
        if elapsed >= calibration_time:
            break

        phase = "AMBIENT" if elapsed < half_time else "ACTIVE"

        if sensor.check_data_ready():
            ir1, ir2, ir3, ir4 = sensor.read_ir_all()
            ir_total = abs(ir1) + abs(ir2) + abs(ir3) + abs(ir4)
            deriv = feature_state.update(ir3)
            if elapsed < half_time:
                ambient_totals.append(ir_total)
                ambient_ir3.append(deriv)
            else:
                active_totals.append(ir_total)
                active_ir3.append(deriv)
        if now - last_print > 0.2:
            pct = int((elapsed / calibration_time) * 50)
            bar = "[" + "#" * pct + " " * (50 - pct) + "]"
            print(f"{bar} Phase: {phase:7}  t={elapsed:4.1f}s", end="\r")
            last_print = now
        time.sleep(0.01)

    print("\n\n" + "=" * 60)
    print("CALIBRATION RESULTS")
    print("=" * 60)
    print(f"Ambient samples: {len(ambient_totals)}, Active samples: {len(active_totals)}")

    if len(ambient_totals) < 10 or len(active_totals) < 10:
        print("Not enough samples collected, using default thresholds.")
        return {
            "total_threshold": 8000,
            "derivative_threshold": 200,
        }

    amb_mean = statistics.mean(ambient_totals)
    amb_std = statistics.pstdev(ambient_totals) if len(ambient_totals) > 1 else 0.0
    act_mean = statistics.mean(active_totals)
    base_total_th = amb_mean + (act_mean - amb_mean) * 0.5
    noise_margin = 3 * amb_std
    total_threshold = int(base_total_th + noise_margin)

    amb_deriv_abs = [abs(x) for x in ambient_ir3 if x is not None]
    act_deriv_abs = [abs(x) for x in active_ir3 if x is not None]
    if len(amb_deriv_abs) < 10 or len(act_deriv_abs) < 10:
        deriv_threshold = 200
    else:
        amb_d_mean = statistics.mean(amb_deriv_abs)
        amb_d_std = statistics.pstdev(amb_deriv_abs) if len(amb_deriv_abs) > 1 else 0.0
        act_d_mean = statistics.mean(act_deriv_abs)
        base_deriv_th = amb_d_mean + (act_d_mean - amb_d_mean) * 0.4
        deriv_margin = 3 * amb_d_std
        deriv_threshold = int(base_deriv_th + deriv_margin)

    print(f"Ambient total: mean={amb_mean:.1f}, std={amb_std:.1f}")
    print(f"Active  total: mean={act_mean:.1f}")
    print(f"Chosen total IR threshold: {total_threshold}")
    if len(amb_deriv_abs) >= 10 and len(act_deriv_abs) >= 10:
        print(f"Ambient |dIR3|: mean={statistics.mean(amb_deriv_abs):.1f}")
        print(f"Active  |dIR3|: mean={statistics.mean(act_deriv_abs):.1f}")
    print(f"Chosen derivative threshold: {deriv_threshold}")

    return {
        "total_threshold": total_threshold,
        "derivative_threshold": deriv_threshold,
    }

def main():
    print("=== AK9753 Indoor Presence Detection (No Firebase) ===")

    # Initialize sensor
    try:
        sensor = AK9753(1)
        sensor.soft_reset()
        if not sensor.begin():
            print("AK9753 sensor initialization failed!")
            exit(1)
        print("AK9753 sensor initialized successfully")
    except Exception as e:
        print(f"Sensor initialization failed: {e}")
        exit(1)

    # Advanced calibration
    cfg = calibrate_sensor(sensor, calibration_time=15)
    motion_state = MotionFeatureState(ea=0.05)

    print("\n" + "="*60)
    print("TESTING WITH CALIBRATED THRESHOLDS")
    print("="*60)
    print(f"Thresholds: IR Total: {cfg['total_threshold']}, Derivative: {cfg['derivative_threshold']}")
    print("Testing for 10 seconds...")
    print("-" * 60)

    start_time = time.time()
    test_detections = 0
    test_readings = 0

    while time.time() - start_time < 10:
        if sensor.check_data_ready():
            temperature_c = get_temperature(sensor)
            ir1, ir2, ir3, ir4 = sensor.read_ir_all()

            print('IR RAW:', ir1, ir2, ir3, ir4)

            presence_detected, ir_total, deriv = detect_presence(
                ir1, ir2, ir3, ir4, cfg, motion_state
            )

            if presence_detected:
                test_detections += 1

            test_readings += 1

            comfort = get_indoor_comfort_level(temperature_c)
            status = "DETECTED" if presence_detected else "NOT DETECTED"
            temp_source = "SenseHAT" if sense else "AK9753"
            print(
                f"IR: {ir_total:6d} | dIR3: {deriv:6d} | Presence: {status:14} | "
                f"Temp: {temperature_c:4.1f}C ({comfort:15}) [{temp_source}]"
            )
            time.sleep(0.1)

    print(f"\nTest Results: {test_detections}/{test_readings} detections")

    print("\n" + "="*60)
    print("STARTING CONTINUOUS MONITORING (Ctrl+C to stop)")
    print("="*60)

    detection_count = 0
    current_session_start = None

    try:
        while True:
            if sensor.check_data_ready():
                temperature_c = get_temperature(sensor)
                ir1, ir2, ir3, ir4 = sensor.read_ir_all()

                presence_detected, ir_total, deriv = detect_presence(
                    ir1, ir2, ir3, ir4, cfg, motion_state
                )

                if presence_detected:
                    if current_session_start is None:
                        current_session_start = datetime.now()
                    detection_count += 1
                    session_duration = (datetime.now() - current_session_start).total_seconds()
                else:
                    current_session_start = None
                    session_duration = 0

                comfort_level = get_indoor_comfort_level(temperature_c)
                room_status = "OCCUPIED" if presence_detected else "VACANT"
                temp_source = "SenseHAT" if sense else "AK9753"

                print(
                    f"Room: {room_status:8} | IR: {ir_total:6d} | dIR3: {deriv:6d} | "
                    f"Temp: {temperature_c:4.1f}C ({comfort_level:15}) | "
                    f"Session: {session_duration:4.0f}s | Detections: {detection_count:3d} "
                    f"[{temp_source}]"
                )

            time.sleep(1)
    except KeyboardInterrupt:
        print("\n\n" + "="*60)
        print("MONITORING STOPPED")
        print("="*60)
        print(f"Total detections: {detection_count}")
        print("Goodbye!")

if __name__ == "__main__":
    main()
