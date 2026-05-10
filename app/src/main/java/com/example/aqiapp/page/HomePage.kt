@file:OptIn( ExperimentalMaterial3Api::class)

package com.example.aqiapp.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqiapp.reusable.*
import com.google.firebase.database.DatabaseReference

@Composable
fun HomePage(navigateTo: (String) -> Unit, database: DatabaseReference, nomorRuangan: String, modifier: Modifier = Modifier) {

    var keterangan by remember { mutableStateOf("") }
    var co by remember { mutableStateOf("") }
    var coLimit by remember { mutableFloatStateOf(0f) }
    var co2 by remember { mutableStateOf("") }
    var co2Limit by remember { mutableFloatStateOf(0f) }
    var pm25 by remember { mutableStateOf("") }
    var pm25Limit by remember { mutableIntStateOf(0) }
    var suhu by remember { mutableStateOf("") }
    var suhuLimit by remember { mutableIntStateOf(0) }
    var kelembapan by remember { mutableStateOf("") }
    var kelembapanLimit by remember { mutableIntStateOf(0) }
    var isFanInActive by remember { mutableStateOf(false) }
    var isFanOutActive by remember { mutableStateOf(false) }
    var lastFanInStatus by remember { mutableStateOf(false) }
    var lastFanOutStatus by remember { mutableStateOf(false) }
    var isAutoLocal by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(nomorRuangan) {
        monitoring(context, database, nomorRuangan){ newCo, newCo2, newPm25, newSuhu, newKelembapan, newKeterangan ->
            co = newCo
            co2 = newCo2
            pm25 = newPm25
            suhu = newSuhu
            kelembapan = newKelembapan
            keterangan = newKeterangan
        }
        fetchSensorLimits (context, database, nomorRuangan){ coLimits, co2Limits, pm25Limits, suhuLimits, kelembapanLimits  ->
            coLimit = coLimits
            co2Limit = co2Limits
            suhuLimit = suhuLimits
            kelembapanLimit = kelembapanLimits
            pm25Limit = pm25Limits
        }
        statusControl(
            context, database, nomorRuangan
        ){ auto, fanInActive, fanOutActive ->
            isAutoLocal = auto
            if (!auto) {
                // Simpan status fan jika Auto mati
                lastFanInStatus = fanInActive
                lastFanOutStatus = fanOutActive
            }
            isFanInActive = fanInActive
            isFanOutActive = fanOutActive
        }
    }

    Scaffold (
        modifier.navigationBarsPadding(),
        bottomBar = {
            //BottomNavBar(navController)
            AppBottomBar(currentRoute = "home", onItemSelected = navigateTo)
        }
    ) { padding ->
        Surface(
            color = Color(0xFFE9F5F2),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            ) {
                Surface(
                    modifier
                        .fillMaxWidth()
                        .height(65.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0, 203, 168, 140),
                    shadowElevation = 10.dp
                ){
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = "Hi... Welcome to $nomorRuangan",
                            fontSize = 20.sp
                        )
                    }
                }
                //container Kontrol
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF0FAFF),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(0.9f)
                ) {
                    // 1. Tombol Kontrol
                    ContrlButtn(
                        statusOtomatisasi = isAutoLocal,
                        fanInActive = isFanInActive,
                        fanOutActive = isFanOutActive,
                        onToggleAuto = { newAuto ->
                            if (newAuto) {
                                lastFanInStatus = isFanInActive
                                lastFanOutStatus = isFanOutActive
                            }
                            isAutoLocal = newAuto
                            updateAutoMode(database, nomorRuangan, newAuto)
                            if (newAuto) {
                                isFanInActive = true
                                isFanOutActive =  true
                                database.child("exhaustFan").child(nomorRuangan).child("fan1")
                                    .child("statusAktivasi").setValue(false)
                                database.child("exhaustFan").child(nomorRuangan).child("fan2")
                                    .child("statusAktivasi").setValue(false)
                            }else{
                                // Restore fan status yang terakhir sebelum Auto diaktifkan
                                database.child("exhaustFan").child(nomorRuangan).child("fan1")
                                    .child("statusAktivasi").setValue(lastFanInStatus)
                                database.child("exhaustFan").child(nomorRuangan).child("fan2")
                                    .child("statusAktivasi").setValue(lastFanOutStatus)
                            }
                        },
                        onToggleFanIn = { newValue ->
                            isFanInActive = newValue
                            database.child("exhaustFan").child(nomorRuangan).child("fan1")
                                .child("statusAktivasi").setValue(newValue)
                        },
                        onToggleFanOut = { newValue ->
                            isFanOutActive = newValue
                            database.child("exhaustFan").child(nomorRuangan).child("fan2")
                                .child("statusAktivasi").setValue(newValue)
                        }
                    )
                }

                Spacer(modifier.height(16.dp))
                //container Monitoring
                Surface(
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {

                        // 1. Data Sensor
                        SensorDataGrid(
                            co = co,
                            co2 = co2,
                            pm25 = pm25,
                            suhu = suhu,
                            kelembapan = kelembapan,
                            coLimit = coLimit,
                            co2Limit = co2Limit,
                            suhuLimit = suhuLimit.toFloat(),
                            kelembapanLimit = kelembapanLimit.toFloat(),
                            pm25Limit = pm25Limit.toFloat(),
                            keterangan = keterangan
                        )

                }
                Spacer(
                    modifier
                        .height(16.dp)
                )
            }
        }
    }
}



