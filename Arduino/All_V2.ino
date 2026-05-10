#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <MQUnifiedsensor.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include "Adafruit_PM25AQI.h"
#include <time.h>

// ==============================
// 🔌 WiFi & Firebase Setup
// ==============================
#define WIFI_SSID "NAMA_WIFI_ANDA"
#define WIFI_PASSWORD "PASSWORD_WIFI_ANDA"

#define API_KEY "API_KEY_ANDA"
#define FIREBASE_AUTH "TOKEN_RAHASIA_FIREBASE"
#define DATABASE_URL "PROJECT_ID.firebaseio.com" // Contoh: aqi-app.firebaseio.com
#define FIREBASE_PROJECT_ID "ID_PROJECT_FIREBASE"// contoh: aaa-12-123ab

const String nomorRuangan = "room01";

// Firebase instance
FirebaseData fbdo;
FirebaseAuth auth;
FirebaseConfig config;

// Stream untuk RTDB
FirebaseData streamData;
FirebaseData limitStreamData;
FirebaseData firebaseData;

// ==============================
// 🔌 Sensor & Pin Setup
// ==============================
#define RELAY1_PIN  14
#define RELAY2_PIN  27

#define Placa "ESP 32"
#define Volt_MQ7 3.3
#define Pin_MQ7 33
#define Type_MQ7 "MQ-7"
#define ADC_Bit_MQ 12
#define Ratio_MQ7_Clean_Air 27.5

#define Voltage_MQ135 3.3
#define Pin_MQ135 35
#define Type_MQ135 "MQ-135"
#define Ratio_MQ135_Clean_Air 3.6

Adafruit_BME280 bme;
Adafruit_PM25AQI aqi = Adafruit_PM25AQI();
MQUnifiedsensor MQ135(Placa, Voltage_MQ135, ADC_Bit_MQ, Pin_MQ135, Type_MQ135);
MQUnifiedsensor MQ7(Placa, Volt_MQ7, ADC_Bit_MQ, Pin_MQ7, Type_MQ7);

// ==============================
// 🔌 Variabel
// ==============================
bool fan1_statusAktivasi, fan1_statusOtomatisasi, fan2_statusAktivasi, fan2_statusOtomatisasi = false;

float limitCO, limitCO2, limitSuhu, limitKelembapan, limitPM25;
float sumCO = 0, sumCO2 = 0, sumSuhu = 0, sumKelembapan = 0, sumPM25 = 0;
float sumCOHarian = 0, sumCO2Harian = 0, sumSuhuHarian = 0, sumKelembapanHarian = 0, sumPM25Harian = 0;

unsigned long lastSensorRead = 0;
unsigned long lastEvaluation = 0;
unsigned long lastSnapshotMillis = 0;

const unsigned long intervalSensorRead = 600;
const unsigned long evaluationPeriod = 6000;
const unsigned long snapshotPeriod = 60UL * 1000UL; // 24 jam

int sampleCount = 0;
int sampleCountHarian = 0;

// ==============================
// 🔌 Setup
// ==============================
void setup() {
  Serial.begin(9600);
  pinMode(RELAY1_PIN, OUTPUT);
  pinMode(RELAY2_PIN, OUTPUT);

  connectWiFi();
  setupNTP();
  setupFirebase();
  setupSensors();
  setupFirebaseStream();
}

// ==============================
// 🔌 Loop
// ==============================
void loop() {
  handleFirebaseStream();
  handleLimitStream();

  unsigned long currentMillis = millis();

  // Baca sensor setiap 600ms
  if (currentMillis - lastSensorRead >= intervalSensorRead) {
    lastSensorRead = currentMillis;

    PM25_AQI_Data data;
    int pm25 = aqi.read(&data) ? data.pm25_env : 0;
    int suhu = bme.readTemperature();
    int kelembapan = bme.readHumidity();
    MQ7.update();
    float ppm_co = MQ7.readSensor();
    MQ135.update();
    float ppm_co2 = MQ135.readSensor();

    sumCO += ppm_co;
    sumCOHarian += ppm_co;
    sumCO2 += ppm_co2;
    sumCO2Harian += ppm_co2;
    sumSuhu += suhu;
    sumSuhuHarian += suhu;
    sumKelembapan += kelembapan;
    sumKelembapanHarian += kelembapan;
    sumPM25 += pm25;
    sumPM25Harian += pm25;
    sampleCount++;
    sampleCountHarian++;

  }

  // Evaluasi tiap 6 detik
  if (currentMillis - lastEvaluation >= evaluationPeriod && sampleCount > 0) {
    lastEvaluation = currentMillis;

    float avgCO = sumCO / sampleCount;
    float avgCO2 = sumCO2 / sampleCount;
    float avgSuhu = sumSuhu / sampleCount;
    float avgKelembapan = sumKelembapan / sampleCount;
    float avgPM25 = sumPM25 / sampleCount;

    bool perluNyalakanFan = (avgCO > limitCO || avgCO2 > limitCO2 || avgSuhu > limitSuhu || avgKelembapan < limitKelembapan || avgPM25 > limitPM25);

    if (fan1_statusOtomatisasi || fan2_statusOtomatisasi) {
      digitalWrite(RELAY1_PIN, perluNyalakanFan ? LOW : HIGH);
      digitalWrite(RELAY2_PIN, perluNyalakanFan ? LOW : HIGH);
    }

    kirimRataRataKeRTD(avgCO, avgCO2, avgSuhu, avgKelembapan, avgPM25, perluNyalakanFan);

    sumCO = sumCO2 = sumSuhu = sumKelembapan = sumPM25 = 0;
    sampleCount = 0;

    Serial.printf("Evaluasi: CO %.2f | CO2 %.2f | Suhu %.2f | Kelembapan %.2f | PM2.5 %.2f\n", avgCO, avgCO2, avgSuhu, avgKelembapan, avgPM25);
  }

  // Snapshot harian ke Firestore
  if (currentMillis - lastSnapshotMillis >= snapshotPeriod) {
    lastSnapshotMillis = currentMillis;
    simpanSnapshotHarian();
    sumCOHarian = sumCO2Harian = sumSuhuHarian = sumKelembapanHarian = sumPM25Harian = 0;
    sampleCountHarian = 0;
  }
}

// ==============================
// 🔌 Function WiFi & Firebase
// ==============================
void connectWiFi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Menghubungkan WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" Terhubung!");
}

void setupNTP() {
  configTime(0, 0, "pool.ntp.org", "time.nist.gov");
  while (time(nullptr) < 100000) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" Waktu sinkron!");
}

void setupFirebase() {
  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;

  if (Firebase.signUp(&config, &auth, "", "")) {
    Serial.println("Firebase SignUp OK");
  } else {
    Serial.printf("SignUp gagal: %s\n", config.signer.signupError.message.c_str());
  }

  // config.host = DATABASE_URL;
  // config.signer.tokens.legacy_token = FIREBASE_AUTH;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void setupFirebaseStream() {
  if (!Firebase.RTDB.beginStream(&streamData, "/exhaustFan/" + nomorRuangan)) {
    Serial.println(fbdo.errorReason());
  }

  if (!Firebase.RTDB.beginStream(&limitStreamData, "/limits/" + nomorRuangan)) {
    Serial.println(limitStreamData.errorReason());
  }
}

// ==============================
// 🔌 Handler Stream & RTDB Update
// ==============================
void handleFirebaseStream() {
    if (!Firebase.RTDB.readStream(&streamData)) {
    Serial.println("Gagal membaca stream:");
    Serial.println(streamData.errorReason());
    return;
  }

  if (streamData.streamAvailable()) {
    Serial.println("Data berubah di Firebase.");

    // ✅ Ambil ulang seluruh data dari node "/exhaustFan/room01"
    FirebaseJson response;
    if (Firebase.RTDB.getJSON(&firebaseData, "/exhaustFan/" + nomorRuangan)) {
      FirebaseJson &allData = firebaseData.jsonObject();
      FirebaseJsonData result;

      if (allData.get(result, "fan1/statusAktivasi"))
        fan1_statusAktivasi = result.boolValue;

      if (allData.get(result, "fan1/statusOtomatisasi"))
        fan1_statusOtomatisasi = result.boolValue;

      if (allData.get(result, "fan2/statusAktivasi"))
        fan2_statusAktivasi = result.boolValue;

      if (allData.get(result, "fan2/statusOtomatisasi"))
        fan2_statusOtomatisasi = result.boolValue;

      // Kendalikan relay berdasarkan data terbaru
      digitalWrite(RELAY1_PIN, fan1_statusAktivasi ? LOW : HIGH);
      digitalWrite(RELAY2_PIN, fan2_statusAktivasi ? LOW : HIGH);

      // Debug
      Serial.println("Status Terkini (ambil ulang dari database):");
      Serial.printf("Fan1 - Aktivasi: %d, Otomatisasi: %d\n", fan1_statusAktivasi, fan1_statusOtomatisasi);
      Serial.printf("Fan2 - Aktivasi: %d, Otomatisasi: %d\n", fan2_statusAktivasi, fan2_statusOtomatisasi);
      Serial.println("---------------------------");
    } else {
      Serial.println("Gagal mengambil data Exhaust Fan:");
      Serial.println(firebaseData.errorReason());
    }
  }
}

void handleLimitStream() {
  if (!Firebase.RTDB.readStream(&limitStreamData)) {
    Serial.println("Gagal membaca stream limits:");
    Serial.println(limitStreamData.errorReason());
    return;
  }

  if (limitStreamData.streamAvailable()) {
    Serial.println("Limit data berubah di Firebase!");

    if (Firebase.RTDB.getJSON(&firebaseData, "/limits/" + nomorRuangan)) {
      FirebaseJson &limitData = firebaseData.jsonObject();
      FirebaseJsonData limitResult;

      if (limitData.get(limitResult, "coLimit"))
        limitCO = limitResult.floatValue;
      if (limitData.get(limitResult, "co2Limit"))
        limitCO2 = limitResult.floatValue;
      if (limitData.get(limitResult, "suhuLimit"))
        limitSuhu = limitResult.floatValue;
      if (limitData.get(limitResult, "kelembapanLimit"))
        limitKelembapan = limitResult.floatValue;
      if (limitData.get(limitResult, "pm2_5Limit"))
        limitPM25 = limitResult.floatValue;

      Serial.println("Limit batas diperbarui:");
      Serial.printf("Limit CO: %.2f ppm\n", limitCO);
      Serial.printf("Limit CO2: %.2f ppm\n", limitCO2);
      Serial.printf("Limit Suhu: %.2f °C\n", limitSuhu);
      Serial.printf("Limit Kelembapan: %.2f %%\n", limitKelembapan);
      Serial.printf("Limit PM2.5: %.2f µg/m3\n", limitPM25);
      Serial.println("---------------------------");
    } else {
      Serial.println("Gagal mengambil data limit:");
      Serial.println(firebaseData.errorReason());
    }
  }
}

void kirimRataRataKeRTD(float avgCO, float avgCO2, float avgSuhu, float avgKelembapan, float avgPM25, bool perluNyalakanFan) {
  String path = "/otomatis/" + nomorRuangan;

  FirebaseJson json;
  json.set("coTerkini", avgCO);
  json.set("co2Terkini", avgCO2);
  json.set("suhuTerkini", avgSuhu);
  json.set("kelembapanTerkini", avgKelembapan);
  json.set("pm2_5Terkini", avgPM25);

  json.set("keteranganIndeksKualitasUdara", perluNyalakanFan ? "Udara Buruk" : "Udara Baik");

  Firebase.RTDB.setJSON(&fbdo, path.c_str(), &json);
}

// ==============================
// 🔌 Snapshot Harian ke Firestore
// ==============================
void simpanSnapshotHarian() {
  float avgCO = sumCOHarian / max(sampleCountHarian, 1);
  float avgCO2 = sumCO2Harian / max(sampleCountHarian, 1);
  float avgSuhu = sumSuhuHarian / max(sampleCountHarian, 1);
  float avgKelembapan = sumKelembapanHarian / max(sampleCountHarian, 1);
  float avgPM25 = sumPM25Harian / max(sampleCountHarian, 1);
  String ket;
  if(avgCO > limitCO || avgCO2 > limitCO2 || avgSuhu > limitSuhu || avgKelembapan < limitKelembapan || avgPM25 > limitPM25){
    ket = "Buruk";
  }else{
    ket = "Baik";
  }
  FirebaseJson content;
  content.set("fields/co/doubleValue", avgCO);
  content.set("fields/co2/doubleValue", avgCO2);
  content.set("fields/suhu/integerValue", int(avgSuhu));
  content.set("fields/kelembapan/integerValue", int(avgKelembapan));
  content.set("fields/pm2_5/integerValue", int(avgPM25));
  content.set("fields/keterangan/stringValue", ket);
  content.set("fields/tanggal/timestampValue", getISOTime());

  String docPath = "riwayatKualitasUdara/" + nomorRuangan + "/harian/" + getTanggal();

  if (Firebase.Firestore.createDocument(&fbdo, FIREBASE_PROJECT_ID, "(default)", docPath.c_str(), content.raw())) {
    Serial.println("✅ Snapshot harian berhasil disimpan ke Firestore!");
  } else {
    Serial.println("❌ Gagal snapshot Firestore: " + fbdo.errorReason());
  }
}

String getISOTime() {
  time_t now = time(nullptr);
  struct tm *timeinfo = gmtime(&now);
  char buf[30];
  strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", timeinfo);
  return String(buf);
}

String getTanggal() {
  time_t now = time(nullptr);
  struct tm *timeinfo = gmtime(&now);
  char buf[11];
  strftime(buf, sizeof(buf), "%Y-%m-%d", timeinfo);
  return String(buf);
}

void setupSensors() {
  Serial1.begin(9600, SERIAL_8N1, 16, 17);
  if (!aqi.begin_UART(&Serial1)) {
    Serial.println("Could not find PM 2.5 sensor!");
    while (1) delay(10);
  }
  if (!bme.begin(0x76)) {
    Serial.println("BME280 tidak terdeteksi!");
    while (1) delay(10);
  }

  MQ7.setRegressionMethod(1);
  MQ7.setA(99.042); MQ7.setB(-1.518); MQ7.init();
  float calcR0_MQ7 = 0;
  for (int i = 0; i < 20; i++) { MQ7.update(); calcR0_MQ7 += MQ7.calibrate(Ratio_MQ7_Clean_Air); }
  MQ7.setR0(calcR0_MQ7 / 20);

  MQ135.setRegressionMethod(1);
  MQ135.setA(110.47); MQ135.setB(-2.862); MQ135.init();
  float calcR0_MQ135 = 0;
  for (int i = 0; i < 20; i++) { MQ135.update(); calcR0_MQ135 += MQ135.calibrate(Ratio_MQ135_Clean_Air); }
  MQ135.setR0(calcR0_MQ135 / 20);
}