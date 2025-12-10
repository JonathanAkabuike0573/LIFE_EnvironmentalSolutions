# SGP30 CO₂/eCO₂ & TVOC Air Quality Sensor - Complete Setup Guide

![SGP30 Sensor](https://github.com/PrototypeZone/ceng317/blob/main/hardware/projects/media/sgp30.jpg)

> **Your working Raspberry Pi + Firebase + Adafruit SGP30 code - Complete documentation with your exact setup**

## Table of Contents

1. [Project Overview](#project-overview)
2. [Your Working Code](#your-working-code)
3. [Hardware Wiring](#hardware-wiring)
4. [Raspberry Pi Setup](#raspberry-pi-setup)
5. [Firebase Configuration](#firebase-configuration)
6. [Running Your Code](#running-your-code)
7. [Data Structure](#data-structure)
8. [Troubleshooting](#troubleshooting)

## Project Overview

**Your exact SGP30 air quality monitoring system** using:
- **Adafruit SGP30 library** (`adafruit_sgp30`)
- **CircuitPython I2C** (`board`, `busio`)
- **Firebase Realtime Database** (`life-environmentalsolution`)
- **Real-time eCO2 (ppm) + TVOC (ppb) + descriptive quality levels**

**Data Flow**: SGP30 → Raspberry Pi → Firebase → Android App

graph LR
SGP30[Adafruit SGP30
I2C 0x58] --> Pi[RPi + CircuitPython]
Pi --> Firebase[life-environmentalsolution
Realtime DB]
Firebase --> App[Android App]


## Your Working Code

**`sgp30_monitor.py`** - Your exact tested code:

import time
import board
import busio
import adafruit_sgp30
import firebase_admin
from firebase_admin import credentials, db
from datetime import datetime

Initialize Firebase - YOUR EXACT CONFIG
cred = credentials.Certificate("/home/pi/CENG317/Test/life-environmentalsolution-firebase-adminsdk-fbsvc-1b81d17b94.json")
firebase_admin.initialize_app(cred, {
"databaseURL": "https://life-environmentalsolution-default-rtdb.firebaseio.com/"
})
ref = db.reference("sgp30_readings")

Create I2C connection
i2c = busio.I2C(board.SCL, board.SDA)
sgp30 = adafruit_sgp30.Adafruit_SGP30(i2c)

print("SGP30 Air Quality Sensor Example")
print("----------------------------------")

sgp30.iaq_init()

print("Waiting 15 seconds for sensor to stabilize...")
for i in range(15):
print(f"{15 - i}s", end="\r")
time.sleep(1)

elapsed_sec = 0

while True:
eCO2, TVOC = sgp30.iaq_measure()

# Your eCO2 quality levels
if eCO2 <= 600:
    co2_desc = "Excellent - fresh air"
elif eCO2 <= 1000:
    co2_desc = "Good - normal indoor air"
elif eCO2 <= 1500:
    co2_desc = "Moderate - consider more ventilation"
elif eCO2 <= 2000:
    co2_desc = "Poor - air feels stuffy"
else:
    co2_desc = "Very Poor - ventilation needed!"

# Your TVOC quality levels
if TVOC <= 65:
    tvoc_desc = "Excellent - very clean air"
elif TVOC <= 220:
    tvoc_desc = "Good - typical indoor air"
elif TVOC <= 660:
    tvoc_desc = "Moderate - some pollutants detected"
elif TVOC <= 2200:
    tvoc_desc = "Poor - noticeable chemical emissions"
else:
    tvoc_desc = "Very Poor - strong pollution or odor"

print(f"eCO2: {eCO2} ppm ({co2_desc}) | TVOC: {TVOC} ppb ({tvoc_desc})")

# Firebase data structure
timestamp = datetime.now().isoformat()
data = {
    "timestamp": timestamp,
    "eCO2": eCO2,
    "TVOC": TVOC,
    "co2_description": co2_desc,
    "tvoc_description": tvoc_desc
}

# Send to Firebase
ref.push(data)

elapsed_sec += 2
time.sleep(2)

# Baseline every 30s
if elapsed_sec % 30 == 0:
    baseline_eCO2, baseline_TVOC = sgp30.get_iaq_baseline()
    print(f"---- Baseline: eCO2: 0x{baseline_eCO2:04x}, TVOC: 0x{baseline_TVOC:04x}")


## Hardware Wiring

**Adafruit SGP30 → Raspberry Pi GPIO**:

| SGP30 Pin | RPi Pin | Function |
|-----------|---------|----------|
| **VCC**   | 1 (3.3V) | Power   |
| **GND**   | 9        | Ground  |
| **SDA**   | 3 (GPIO2)| I²C Data|
| **SCL**   | 5 (GPIO3)| I²C Clock|

**Verify**: `sudo i2cdetect -y 1` → Should show `58`

## Raspberry Pi Setup

Enable I2C & SPI
sudo raspi-config # Interface Options → I2C → Enable
sudo reboot

Install CircuitPython libs
sudo apt update
sudo apt install -y python3-pip i2c-tools
pip3 install adafruit-circuitpython-sgp30 firebase-admin


## Firebase Configuration

**Your exact paths**:
- **Service Account**: `/home/pi/CENG317/Test/life-environmentalsolution-firebase-adminsdk-fbsvc-1b81d17b94.json`
- **Database URL**: `https://life-environmentalsolution-default-rtdb.firebaseio.com/`
- **Path**: `sgp30_readings`

## Running Your Code

cd /home/pi/CENG317/Test/
python3 sgp30_monitor.py


**Expected Output**:
SGP30 Air Quality Sensor Example
Waiting 15 seconds for sensor to stabilize...
eCO2: 450 ppm (Excellent - fresh air) | TVOC: 23 ppb (Excellent - very clean air)
---- Baseline: eCO2: 0x0280, TVOC: 0x0098


## Data Structure

**Firebase Realtime Database** (`sgp30_readings`):

{
"sgp30_readings": {
"-Nxxxxx": {
"timestamp": "2025-12-09T22:51:00.123456",
"eCO2": 450,
"TVOC": 23,
"co2_description": "Excellent - fresh air",
"tvoc_description": "Excellent - very clean air"
}
}
}


## Android App Integration

**Listen to**: `sgp30_readings` path
**Display**:
- Live eCO2/TVOC gauges
- Quality descriptions (Excellent/Good/Moderate/Poor/Very Poor)
- Color-coded status indicators

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `No module 'adafruit_sgp30'` | `pip3 install adafruit-circuitpython-sgp30` |
| No `58` on `i2cdetect` | Check 3.3V power, SDA/SCL wiring |
| Firebase auth error | Verify JSON file path exists |
| Always 400ppm/0ppb | Wait full 15s warmup, breathe near sensor |
| `OSError: [Errno 121]` | I2C bus conflict, reboot + check wiring |

---
