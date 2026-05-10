package com.example.aqiapp.reusable

import java.util.Date


data class AirQualityData (
    val tanggal: Date = Date(),
    val co: Double = 0.0,
    val co2: Double = 0.0,
    val suhu: Int = 0,
    val kelembapan: Int = 0,
    val pm2_5: Int = 0,
    val keterangan: String = ""
)