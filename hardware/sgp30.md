# SGP30 CO₂/eCO₂ & TVOC Air Quality Sensor - Complete Hardware Setup Guide

![Photo](https://github.com/PrototypeZone/ceng317/blob/main/hardware/projects/media/sgp30.jpg)

> **Complete instructions for building your own SGP30-based indoor air quality monitoring system with Raspberry Pi, Firebase integration, and optional Android app connectivity**

## Table of Contents

1. [Project Overview](#project-overview)  
2. [Bill of Materials (BOM)](#bill-of-materials-bom)  
3. [Hardware Assembly](#hardware-assembly)  
4. [Raspberry Pi Setup](#raspberry-pi-setup)  
5. [Firebase Configuration](#firebase-configuration)  
6. [Python Code Setup](#python-code-setup)  
7. [Android App Integration](#android-app-integration)  
8. [PCB Design (Optional)](#pcb-design-optional)  
9. [3D Printed Case (Optional)](#3d-printed-case-optional)  
10. [Troubleshooting](#troubleshooting)

---

## Project Overview

This project creates a complete indoor air quality monitoring system using the **Adafruit SGP30** gas sensor to measure **equivalent CO₂ (eCO₂)** in ppm and **TVOC** in ppb.  
Readings are taken on a Raspberry Pi with Python (CircuitPython libraries), pushed to a **Firebase Realtime Database**, and can be displayed on an Android app or web dashboard.

**System Architecture**
flowchart LR;
SGP30_Sensor--->Raspberry_Pi_(Python)--->Firebase_Database--->Android_App;

(I²C) (WiFi) (Cloud) (Mobile)

---

## Bill of Materials (BOM)

**Electronics Components**

Based on your DigiKey orders (same accessories as previous projects, but with the SGP30 CO₂/eCO₂ & TVOC Air Quality Sensor):

|     |                      |                      |                                               |                  |         |
|-----|----------------------|----------------------|-----------------------------------------------|------------------|---------|
| Qty | Part Number (DigiKey)| Manufacturer         | Description                                   | Unit Price (CAD) | Total   |
| 1   | **1528-2531-ND**     | Adafruit Industries  | **SGP30 CO₂/eCO₂ & TVOC Air Quality Sensor**  | ~$27.81          | ~$27.81 |
| 1   | 1528-1783-ND         | Adafruit Industries  | Stacking Header for Raspberry Pi (2x20 GPIO)  | $4.30            | $4.30   |
| 1   | 455-1721-ND          | JST Sales America    | JST Connector Header RA 4POS 2mm (S4B-PH-K-S) | $0.22            | $0.22   |
| 1   | 1568-22726-ND        | SparkFun Electronics | Flexible Qwiic Cable - Female Jumper (4-pin)  | $2.84            | $2.84   |
| 1   | 1528-4528-ND         | Adafruit Industries  | 4-Pin STEMMA/GROVE to Qwiic Cable (400mm)     | $2.84            | $2.84   |
| 1   | 1528-5385-ND         | Adafruit Industries  | STEMMA QT/Qwiic Cable 400mm                   | $2.19            | $2.19   |
| 4   | 732-10422-ND         | Wurth Electronics    | M2.5x16mm Hex Standoffs (Steel)               | $1.02            | $4.08   |
| 4   | 145-50M025045I016-ND | Essentra Components  | M2.5x0.45 Machine Screws (Flat Phil)          | $0.22            | $0.88   |

**Subtotal:** ~$42.00 CAD (approx)

**Additional Required Items (Not in orders)**

|                  |                                                                         |             |
|------------------|-------------------------------------------------------------------------|-------------|
| Item             | Description                                                             | Est. Price  |
| **Raspberry Pi** | Raspberry Pi 4 Model B (2GB or 4GB recommended)                         | $45–55 USD  |
| **MicroSD Card** | 32GB or larger, Class 10                                                | $10–15 USD  |
| **Power Supply** | Official Raspberry Pi USB‑C Power Supply (5V 3A)                        | $8–10 USD   |
| **Jumper Wires** | Male‑to‑Female jumper wires (if not using Qwiic/STEMMA directly)        | $5–8 USD    |

**Total Project Cost:** ~$120–140 USD

---

## Hardware Assembly

### Option 1: Using Qwiic/STEMMA System (Recommended – No Soldering)

The easiest method is to use the Adafruit STEMMA QT / SparkFun Qwiic ecosystem (plug‑and‑play).

**Step 1: Install Stacking Header on Raspberry Pi**

1. If your Raspberry Pi does not have GPIO headers pre‑installed, solder the **2x20 stacking header** onto the GPIO pins.  
2. This allows you to plug the SGP30 breakout while keeping other GPIO pins accessible.

**Step 2: Connect SGP30 with STEMMA QT/Qwiic Cable**

1. Take the **STEMMA QT/Qwiic cable** (1528‑5385‑ND).  
2. Connect one end to the **STEMMA/Qwiic connector** on the SGP30 breakout board.  
3. Connect the other end to the Raspberry Pi GPIO pins (using a Qwiic‑to‑female cable or a Qwiic HAT):

   - **Black wire (GND)** → Pin 9 (Ground)  
   - **Red wire (V+)** → Pin 1 (3.3V)  
   - **Blue wire (SDA)** → Pin 3 (GPIO 2 – I²C SDA)  
   - **Yellow wire (SCL)** → Pin 5 (GPIO 3 – I²C SCL)

**Qwiic/STEMMA QT Cable Pinout Reference**

| Cable Wire | Label on Cable | SGP30 Pin |
|-----------|----------------|-----------|
| Black     | GND            | GND       |
| Red       | V+             | VCC       |
| Blue      | SDA            | SDA       |
| Yellow    | SCL            | SCL       |

**Raspberry Pi GPIO Pin Layout (I²C pins only)**

| Signal | Pi Pin | GPIO | Notes        |
|--------|--------|------|--------------|
| 3V3    | 1      | —    | 3.3 V power  |
| SDA    | 3      | 2    | I²C SDA      |
| SCL    | 5      | 3    | I²C SCL      |
| GND    | 9      | —    | Ground       |


### Option 2: Direct GPIO Connection (Manual Wiring)

If you do not have Qwiic/STEMMA cables:

1. Use **female‑to‑female jumper wires**.  
2. Connect SGP30 breakout pins directly to Raspberry Pi GPIO:

| SGP30 Pin | Wire Color | Raspberry Pi Pin | Function  |
|-----------|------------|------------------|-----------|
| **VCC**   | Red        | Pin 1 (3.3V)     | Power     |
| **GND**   | Black      | Pin 9 (Ground)   | Ground    |
| **SDA**   | Blue       | Pin 3 (GPIO 2)   | I²C Data  |
| **SCL**   | Yellow     | Pin 5 (GPIO 3)   | I²C Clock |

**Important Hardware Notes**

⚠️ **Critical Warnings:**

1. Use **3.3V** logic on the Raspberry Pi; the SGP30 breakout accepts 3.3–5V on VCC but I²C lines are 3.3V when used with the Pi.  
2. Keep the sensor exposed to room air (do not seal it in an airtight box) for accurate gas measurements.  
3. Avoid touching the sensor area with fingers, as oils or contaminants can affect readings.

**SGP30 I²C Address**

- Default I²C address: `0x58` (fixed on the SGP30).  

To verify the sensor is connected:

sudo i2cdetect -y 1


You should see **58** in the grid.

---

## Raspberry Pi Setup

### Step 1: Install Raspberry Pi OS

1. Download **Raspberry Pi Imager**.  
2. Flash **Raspberry Pi OS (64‑bit)** to your microSD card.  
3. Enable SSH and configure Wi‑Fi during imaging or on first boot.

### Step 2: Initial System Configuration

Connect via SSH or local terminal and run:

sudo apt update
sudo apt upgrade -y


Install required system packages:
sudo apt install -y python3-pip python3-dev git i2c-tools

### Enable I²C Interface

1. Run `sudo raspi-config`.  
2. Go to **Interface Options** → **I2C** → enable.  
3. Reboot:
sudo reboot


---

## Firebase Configuration

1. Go to **Firebase Console** and create a new project (you used `life-environmentalsolution`).  
2. In **Realtime Database**, create a database and choose a region.  
3. For quick testing, you can use very open rules (lock them down later):

{
"rules": {
".read": true,
".write": true
}
}


4. Go to **Project Settings → Service Accounts**, click **Generate new private key**, and save the JSON file to your Pi, e.g.:
/home/pi/CENG317/Test/life-environmentalsolution-firebase-adminsdk-fbsvc-1b81d17b94.json

5. Note your **database URL**, for example:
https://life-environmentalsolution-default-rtdb.firebaseio.com/


---

## Python Code Setup

### Step 1: Install Python Libraries
pip3 install adafruit-circuitpython-sgp30 firebase-admin


### Step 2: Create the Python Script
You can find the SGP30 Python script [here](hardware/sgp30.py).
Create `sgp30_monitor.py` with your working code:
import time
import board
import busio
import adafruit_sgp30
import firebase_admin
from firebase_admin import credentials, db
from datetime import datetime

Initialize Firebase
```
cred = credentials.Certificate(
"/home/pi/CENG317/Test/life-environmentalsolution-firebase-adminsdk-fbsvc-1b81d17b94.json"
)
firebase_admin.initialize_app(cred, {
"databaseURL": "https://life-environmentalsolution-default-rtdb.firebaseio.com/"
})
ref = db.reference("sgp30_readings")
```
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

# Interpret eCO2 levels (in ppm)
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

# Interpret TVOC levels (in ppb)
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

# Prepare data
timestamp = datetime.now().isoformat()
data = {
    "timestamp": timestamp,
    "eCO2": eCO2,
    "TVOC": TVOC,
    "co2_description": co2_desc,
    "tvoc_description": tvoc_desc
}

# Send data to Firebase
ref.push(data)

elapsed_sec += 2
time.sleep(2)

# Show baseline every 30 seconds (optional)
if elapsed_sec % 30 == 0:
    baseline_eCO2, baseline_TVOC = sgp30.get_iaq_baseline()
    print(f"---- Baseline values: eCO2: 0x{baseline_eCO2:04x}, TVOC: 0x{baseline_TVOC:04x}")



### Step 3: Run the Script

From the folder where your script lives:
cd /home/pi/CENG317/Test/
python3 sgp30_monitor.py


You should see live eCO₂/TVOC readings printed every 2 seconds and new entries appearing under `sgp30_readings` in Firebase.

---

## Android App Integration

The Android app (or any client) will listen to the Firebase Realtime Database.

**Data Structure (Firebase)**

A typical node under `sgp30_readings` looks like:


{
"timestamp": "2025-12-09T22:51:00.123456",
"eCO2": 450,
"TVOC": 23,
"co2_description": "Excellent - fresh air",
"tvoc_description": "Excellent - very clean air"
}


**App Logic (Java/Kotlin or Flutter, etc.)**

- Connect to Firebase Realtime Database.  
- Attach a listener to the `sgp30_readings` path.  
- Display:
  - Live **eCO₂ (ppm)** and **TVOC (ppb)** values.  
  - The description strings as a color‑coded status (e.g., green for Excellent/Good, yellow for Moderate, red for Poor/Very Poor).

---

## PCB Design (Optional)

If you want a custom PCB:

- Break out the SGP30 footprint or header for the Adafruit SGP30 breakout.  
- Route VCC, GND, SDA, and SCL to a 40‑pin Raspberry Pi compatible header.  
- Add mounting holes that match your standoffs and enclosure.

---

## 3D Printed Case (Optional)

You can design a case that:

- Holds the Raspberry Pi and SGP30 board securely.  
- Provides vent openings around the sensor so air can circulate freely.  
- Exposes USB, HDMI, and power ports as needed.

---

## Troubleshooting

| Issue                                          | Solution                                                                                     |
|-----------------------------------------------|----------------------------------------------------------------------------------------------|
| **No module `adafruit_sgp30`**                | Install the library with `pip3 install adafruit-circuitpython-sgp30`.                        |
| **Sensor not found (no 0x58 in `i2cdetect`)** | Check wiring (SDA/SCL swapped?). Ensure I²C is enabled in `raspi-config`. Confirm 3.3V and GND. |
| **Firebase auth or permission error**         | Verify the service account `.json` path and `databaseURL` string. Check that Pi has internet.|
| **Always 400 ppm eCO₂ / 0 ppb TVOC**          | Let the sensor warm up at least 15 seconds and keep it powered continuously while testing.   |
| **`OSError: [Errno 121]` on I²C**             | Indicates I²C bus error. Reboot the Pi, recheck SDA/SCL/VCC/GND, and ensure only one device at 0x58. |

---


