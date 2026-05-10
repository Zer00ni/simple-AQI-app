package com.example.aqiapp.page

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.aqiapp.reusable.*
import com.google.firebase.database.FirebaseDatabase

@Composable
fun SettingPage(navController: NavController, navigateTo: (String) -> Unit, modifier: Modifier = Modifier, nomorRuangan: String) {

    val context = LocalContext.current
    var setLimitDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var information by remember { mutableStateOf(false) }
    val database = FirebaseDatabase.getInstance("https://aqi-00-138ef-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    var coLimit by remember { mutableFloatStateOf(0f) }
    var co2Limit by remember { mutableFloatStateOf(0f) }
    var pm25Limit by remember { mutableIntStateOf(0) }
    var suhuLimit by remember { mutableIntStateOf(0) }
    var kelembapanLimit by remember { mutableIntStateOf(0) }

    LaunchedEffect(nomorRuangan) {
        fetchSensorLimits (context, database, nomorRuangan){ coLimits, co2Limits, pm25Limits, suhuLimits, kelembapanLimits  ->
            coLimit = coLimits
            co2Limit = co2Limits
            suhuLimit = suhuLimits
            kelembapanLimit = kelembapanLimits
            pm25Limit = pm25Limits
        }
    }

    Scaffold(
        modifier
            .navigationBarsPadding(),
        bottomBar = {
            AppBottomBar(currentRoute = "setting", onItemSelected = navigateTo)
        },
        containerColor = Color(0xFFE9F5F2)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier.fillMaxWidth().padding(start = 5.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Setting",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = {
                            confirmLogout = true
                        },
                        modifier
                        //.align(Alignment.Bottom)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            Modifier.size(33.dp)
                        )
                    }
                    Information(
                        showDialog = information,
                        onDismiss = { information = false }
                    )
                    ConfirmLogout(
                        showDialog = confirmLogout,
                        onConfirm = {
                            confirmLogout = false
                            navController.navigate("login") { popUpTo(0) }
                        },
                        onDismiss = { confirmLogout = false }
                    )
                }
                Surface(
                    modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    color = Color(0xFFE0F7FA),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier.padding(top = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Limits", fontSize = 25.sp, fontFamily = FontFamily.SansSerif)
                                TampilLimit(
                                    co = coLimit,
                                    co2 = co2Limit,
                                    suhu = suhuLimit,
                                    kelembapan = kelembapanLimit,
                                    pm25 = pm25Limit
                                )
                            }
                        }
                        Spacer(modifier.height(10.dp))
                        Surface(
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement =  Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        setLimitDialog = true
                                    },
                                    modifier
                                        .size(200.dp, 40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00695C)
                                    )
                                ) {
                                    Text("Set New Limits", fontSize = 17.sp, color = Color.White)
                                }

                                SetLimitDialog(
                                    context = context,
                                    showDialog = setLimitDialog,
                                    onDismiss = { setLimitDialog = false },
                                    onSendSet = { data ->
                                        database.child("limits").child(nomorRuangan).setValue(data)
                                            .addOnSuccessListener {
                                                Log.d("Firebase", "Data berhasil disimpan")
                                                Toast.makeText(
                                                    context,
                                                    "Limit Berhasil di Perbaharui!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firebase", "Gagal menyimpan data", e)
                                                Toast.makeText(
                                                    context,
                                                    "Limit Gagal di Perbaharui!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                )

                                Spacer(modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        resetDialog = true
                                    },

                                    modifier
                                        .size(200.dp, 40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00695C)
                                    )
                                ) {
                                    Text("Reset Limit", fontSize = 17.sp, color = Color.White)
                                }
                                ConfirmReset(
                                    showDialog = resetDialog,
                                    onDismiss = { resetDialog = false },
                                    onSendSet = {
                                        database.child("limits").child(nomorRuangan)
                                            .setValue(defaultLimits)
                                            .addOnSuccessListener {
                                                Log.d("Firebase", "Limit berhasil di-reset ke default")
                                                Toast.makeText(
                                                    context,
                                                    "Limit Berhasil di Reset!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firebase", "Gagal reset limit", e)
                                                Toast.makeText(
                                                    context,
                                                    "Limit Gagal di Reset!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = { information = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = "Information",
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFF00695C)
                )
            }
        }
    }
}