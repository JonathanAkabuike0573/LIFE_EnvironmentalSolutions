import time
import board
import busio
import adafruit_sgp30
import firebase_admin
from firebase_admin import credentials, db
from datetime import datetime

# Initialize Firebase
cred = credentials.Certificate("/home/pi/CENG317/Test/life-environmentalsolution-firebase-adminsdk-fbsvc-1b81d17b94.json")
firebase_admin.initialize_app(cred, {
    "databaseURL": "https://life-environmentalsolution-default-rtdb.firebaseio.com/"
})
ref = db.reference("sgp30_readings")

# Create I2C connection
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
