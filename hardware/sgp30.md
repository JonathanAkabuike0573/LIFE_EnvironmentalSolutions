# SGP30 CO₂/eCO₂ & TVOC Air Quality Sensor - Complete Hardware Setup Guide

![Photo](https://github.com/PrototypeZone/ceng317/blob/main/hardware/projects/media/sgp30.jpg)

> **Complete instructions for building your own SGP30-based indoor air quality monitoring system with Raspberry Pi, Firebase integration, and optional Android app connectivity**

## Table of Contents

1.  [Project Overview](#project-overview)
2.  [Bill of Materials (BOM)](#bill-of-materials-bom)
3.  [Hardware Assembly](#hardware-assembly)
4.  [Raspberry Pi Setup](#raspberry-pi-setup)
5.  [Firebase Configuration](#firebase-configuration)
6.  [Python Code Setup](#python-code-setup)
7.  [Android App Integration](#android-app-integration)
8.  [PCB Design (Optional)](#pcb-design-optional)
9.  [3D Printed Case (Optional)](#3d-printed-case-optional)
10. [Troubleshooting](#troubleshooting)

## Project Overview

This project creates a complete indoor air quality monitoring system using the **Adafruit SGP30** gas sensor to measure **equivalent CO₂ (eCO₂)** in ppm and **TVOC** in ppb.  
Readings are taken on a Raspberry Pi with Python (CircuitPython libraries), pushed to a **Firebase Realtime Database**, and can be displayed on an Android app or web dashboard.

**System Architecture**

flowchart LR;
SGP30_Sensor--->Raspberry_Pi_(Python)--->Firebase_Database--->Android_App;


(I2C) (WiFi) (Cloud) (Mobile)

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

## Hardware Assembly

**Option 1: Using Qwiic/STEMMA System (Recommended – No Soldering)**

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

Qwiic/STEMMA QT Connector:
┌─────────────────┐
│ BLK RED BLU YEL │
│ GND V+ SDA SCL │
└─────────────────┘

**Raspberry Pi GPIO Pin Layout (Top View)**

┌───────────────┐
3V3 │ 1 ● ● 2 │ 5V
SDA │ 3 ● ● 4 │ 5V
SCL │ 5 ● ● 6 │ GND
│ 7 ● ● 8 │
GND │ 9 ● ●10 │
│ ... │
└───────────────┘


**Option 2: Direct GPIO Connection (Manual Wiring)**

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

## Raspberry Pi Setup

**Step 1: Install Raspberry Pi OS**

