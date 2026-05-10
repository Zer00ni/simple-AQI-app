package com.example.aqiapp.reusable

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.aqiapp.R


object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTING = "setting"
}

@Composable
fun AppBottomBar(currentRoute: String,onItemSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(
        modifier
            .height(64.dp),
        containerColor = Color.Transparent
    ) {
        NavigationBarItem(
            icon = {Icon(Icons.Default.Refresh, contentDescription = "History")},
            label = {Text("History")},
            selected = currentRoute == Routes.HISTORY,
            onClick = {onItemSelected(Routes.HISTORY)}
        )
        NavigationBarItem(
            icon = {Icon(Icons.Default.Home, contentDescription = "Home")},
            label = {Text("Home")},
            selected = currentRoute == Routes.HOME,
            onClick = {onItemSelected(Routes.HOME)}
        )
        NavigationBarItem(
            icon = {Icon(Icons.Default.Settings, contentDescription = "Setting")},
            label = {Text("Setting")},
            selected = currentRoute == Routes.SETTING,
            onClick = {onItemSelected(Routes.SETTING)}
        )
    }

}

fun checkLogin(
    context: Context,
    database: DatabaseReference,
    inputKodeAkses: String,
    onLoginSuccess: (String) -> Unit
) {
    if (inputKodeAkses.isBlank()) {
        Toast.makeText(context, "Kode Akses harus diisi!", Toast.LENGTH_SHORT).show()
        return
    }
    database.child("user").child(inputKodeAkses).addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                val nomorRuangan = snapshot.child("nomorRuangan").getValue(String::class.java)
                if(nomorRuangan != null){
                    //login berhasil
                    Toast.makeText(context, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess(nomorRuangan)
                }else{
                    Toast.makeText(context, "Kode Akses Tidak Terdaftar Dengan Ruangan Manapun!", Toast.LENGTH_SHORT).show()
                }
            }else {
                    Toast.makeText(context, "Kode Akses Tidak Terdaftar!", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context, "Gagal ambil data: ${error.message}", Toast.LENGTH_LONG).show()
        }
    })
}

fun monitoring(
    context: Context,
    database: DatabaseReference,
    nomorRuangan: String,
    onDataUpdate: (
        co: String,
        co2: String,
        pm25: String,
        suhu: String,
        kelembapan: String,
        keterangan : String
        ) -> Unit
){
    val otomatisRef = database.child("otomatis").child(nomorRuangan)
    otomatisRef.addValueEventListener(object : ValueEventListener{
        @SuppressLint("DefaultLocale")
        override fun onDataChange(snapshot: DataSnapshot) {

            val co = snapshot.child("coTerkini").getValue(Double::class.java)?.let { "%.2f".format(it) } ?: "xx ppm"
            val co2 = snapshot.child("co2Terkini").getValue(Double::class.java)?.let { "%.2f".format(it) } ?: "xx ppm"
            val pm25 = snapshot.child("pm2_5Terkini").getValue(Long::class.java)?.toString() ?: "xx µg/m³"
            val suhu = snapshot.child("suhuTerkini").getValue(Long::class.java)?.toString() ?: "xx°C"
            val kelembapan = snapshot.child("kelembapanTerkini").getValue(Long::class.java)?.toString() ?: "xx%"
            val keterangan = snapshot.child("keteranganIndeksKualitasUdara").getValue(String::class.java)?: "xx"
            onDataUpdate(co, co2, pm25, suhu, kelembapan, keterangan)
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Gagal Mengambil data: ${error.message}")
            Toast.makeText(context,"Gagal mengambil data: ${error.message}", Toast.LENGTH_LONG).show()
        }
    })
}

fun fetchSensorLimits(
    context: Context,
    database: DatabaseReference,
    nomorRuangan: String,
    onDataUpdate: (
        co: Float,
        co2: Float,
        pm25: Int,
        suhu: Int,
        kelembapan: Int
    ) -> Unit
){
    val limitsRef = database.child("limits").child(nomorRuangan)

    limitsRef.addValueEventListener(object : ValueEventListener{
        @SuppressLint("DefaultLocale")
        override fun onDataChange(snapshot: DataSnapshot) {

            val co = snapshot.child("coLimit").getValue(Double::class.java) !!
            val co2 = snapshot.child("co2Limit").getValue(Double::class.java) !!
            val pm25 = snapshot.child("pm2_5Limit").getValue(Long::class.java) !!
            val suhu = snapshot.child("suhuLimit").getValue(Long::class.java) !!
            val kelembapan = snapshot.child("kelembapanLimit").getValue(Long::class.java) !!
            onDataUpdate(co.toFloat(), co2.toFloat(), pm25.toInt(), suhu.toInt(), kelembapan.toInt())
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context,"Gagal mengambil data: ${error.message}", Toast.LENGTH_LONG).show()
        }
    })
}

fun statusControl(
    context: Context,
    database: DatabaseReference,
    nomorRuangan: String,
    onDataUpdate: (
        statusAuto : Boolean,
        statusFI : Boolean,
        statusFO : Boolean
    ) -> Unit
){
    val exhaustFanRef = database.child("exhaustFan").child(nomorRuangan)
    exhaustFanRef.addValueEventListener(object : ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            val fanInAuto = snapshot.child("fan1/statusOtomatisasi").getValue(Boolean::class.java) == true
            val fanOutAuto = snapshot.child("fan2/statusOtomatisasi").getValue(Boolean::class.java) == true
            val fanInActive = snapshot.child("fan1/statusAktivasi").getValue(Boolean::class.java) == true
            val fanOutActive = snapshot.child("fan2/statusAktivasi").getValue(Boolean::class.java) == true

            val auto = fanInAuto && fanOutAuto
            onDataUpdate(auto, fanInActive, fanOutActive)
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(context,"Gagal mengambil data: ${error.message}", Toast.LENGTH_LONG).show()
        }
    })
}
fun updateAutoMode(
    database: DatabaseReference,
    nomorRuangan: String,
    isAuto: Boolean
){
    val fan1Ref = database.child("exhaustFan").child(nomorRuangan).child("fan1").child("statusOtomatisasi")
    val fan2Ref = database.child("exhaustFan").child(nomorRuangan).child("fan2").child("statusOtomatisasi")

    fan1Ref.setValue(isAuto)
    fan2Ref.setValue(isAuto)
}

@Composable
fun TampilLimit(
    co: Float,
    co2: Float,
    suhu: Int,
    kelembapan: Int,
    pm25: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(2.dp, Color(0xFF00B894), RoundedCornerShape(12.dp)),
        color = Color(0xFFCFF5E7),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LabelValue(label = "CO", value = "$co ppm")
            LabelValue(label = "CO₂", value = "$co2 ppm")
            LabelValue(label = "Suhu", value = "$suhu °C")
            LabelValue(label = "PM2.5", value = "$pm25 µg/m³")
            LabelValue(label = "Kelembapan", value = "$kelembapan %")
        }
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 16.sp, color = Color.Black)
        Text(text = value, fontSize = 16.sp, color = Color.DarkGray)
    }
}

//SetControl
@Composable
fun ContrlButtn(
    statusOtomatisasi: Boolean,
    fanInActive: Boolean,
    fanOutActive: Boolean,
    onToggleAuto: (Boolean) -> Unit,
    onToggleFanIn: (Boolean) -> Unit,
    onToggleFanOut: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressedAuto by interactionSource.collectIsPressedAsState()
    val scaleAuto by animateFloatAsState(if (pressedAuto) 0.9f else 1f)

    //Parent
    Surface(
        modifier
            .fillMaxWidth(),
        color = Color(0xFFE0F7FA),
        //.border(1.dp, color = Color.Red)
    ) {
        Column(
            modifier
                .fillMaxWidth()
        ) {
            //container atas
            Surface(
                modifier
                    .fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Tombol Fan-In
                    FanSwitch(
                        "Fan-In",
                        fanInActive,
                        statusOtomatisasi,
                        onToggle = {onToggleFanIn(!fanInActive)},
                        image = if (statusOtomatisasi){
                            painterResource(id = R.drawable.faninw)
                        }else if(fanInActive) {
                            painterResource(id = R.drawable.faninw)
                        }else{
                            painterResource(id = R.drawable.faninb)
                        }
                    )
                    // Tombol Fan-Out
                    FanSwitch(
                        "Fan-Out",
                        fanOutActive,
                        statusOtomatisasi,
                        onToggle = {onToggleFanOut(!fanOutActive)},
                        image = if (statusOtomatisasi){
                            painterResource(id = R.drawable.fanoutw)
                        }else if(fanOutActive) {
                            painterResource(id = R.drawable.fanoutw)
                        }else{
                            painterResource(id = R.drawable.fanoutb)
                        }
                    )
                }
            }
            Column (
                modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Tombol Auto (Persegi Panjang)
                Button(
                    onClick = {
                        onToggleAuto(!statusOtomatisasi)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(200.dp)
                        .height(100.dp)
                        .scale(scaleAuto)
                        .padding(bottom = 26.dp),
                    interactionSource = interactionSource,

                    colors = ButtonDefaults.buttonColors(
                        //containerColor = Color.Green
                        containerColor = if (statusOtomatisasi) Color(0xFF00B894) else Color(0xFFB2BEC3)
                    ),
                ) {
                Text("AUTO", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00695C), fontSize = 23.sp)
                }
            }
        }
    }
}

@Composable
fun FanSwitch(
    nama: String,
    statusAktivasi: Boolean,
    auto: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,// Berubah untuk menerima status baru
    image: Painter? = null,
) {
    var checked = if (auto) true else statusAktivasi

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(35.dp), // Ukuran gambar disesuaikan, bisa kamu ubah sesuai kebutuhan
                        //.padding(start = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = nama,
                color = if (auto) Color(0xFF00695C) else if (checked) Color(0xFF00695C) else Color.Black,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                    checked = it
                    onToggle(it) // Panggil callback dengan nilai baru
            },
            enabled = !auto, // Disable switch saat mode auto aktif
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00B894),
                checkedTrackColor = Color(0xFF00B894).copy(alpha = 0.5f),
                uncheckedThumbColor = Color(0xFFB2BEC3),
                uncheckedTrackColor = Color(0xFFB2BEC3).copy(alpha = 0.5f),
                disabledCheckedThumbColor = Color(0xFF00B894),  // Warna gray saat unchecked + disabled
                disabledCheckedTrackColor = Color(0xFF00B894).copy(alpha = 0.3f),
                disabledCheckedBorderColor = Color(0xFF00695C)

            )
        )
    }
}

@Composable
fun Sensor(title: String, value: String, image: Painter? = null, modifier: Modifier = Modifier, fontSize: TextUnit = 17.sp, textColor: Color = Color.Black) {
    Card(
        modifier = modifier
            .heightIn(min = 40.dp)
            //.shadow(4.dp, RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) // efek bayangan bawah
            .border(1.dp, Color(0xFF00695C), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCFF5E7)),

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (image != null) {
                Image(
                    painter = image,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(40.dp) // Ukuran gambar disesuaikan, bisa kamu ubah sesuai kebutuhan
                        .padding(start = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Column(
                modifier
                    .widthIn(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    modifier = Modifier
                        .widthIn(min = 10.dp)
                        .wrapContentHeight(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = value,
                    modifier = Modifier
                        //.padding(12.dp)
                        .wrapContentHeight(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor

                )
            }
        }
    }
}

@Composable
fun SensorDataGrid(
    co: String,
    co2: String,
    suhu: String,
    kelembapan: String,
    pm25: String,
    coLimit: Float,
    co2Limit: Float,
    suhuLimit: Float,
    kelembapanLimit: Float,
    pm25Limit: Float,
    keterangan: String,
    modifier: Modifier = Modifier
) {
    val coValue = co.toFloatOrNull() ?: 0f
    val coColor = if (coValue > coLimit) Color.Red else Color(0xFF00695C)

    val co2Value = co2.toFloatOrNull() ?: 0f
    val co2Color = if (co2Value > co2Limit) Color.Red else Color(0xFF00695C)

    val pm25Value = pm25.toFloatOrNull() ?: 0f
    val pm25Color = if (pm25Value > pm25Limit) Color.Red else Color(0xFF00695C)

    val suhuValue = suhu.toFloatOrNull() ?: 0f
    val suhuColor = if (suhuValue > suhuLimit) Color.Red else Color(0xFF00695C)

    val kelembapanValue = kelembapan.toFloatOrNull() ?: 0f
    val kelembapanColor = if (kelembapanValue < kelembapanLimit) Color.Red else Color(0xFF00695C)

    Surface(
        modifier
            .fillMaxWidth()
            .height(205.dp),
        color = Color(0xFFE0F7FA),
        //.border(1.dp, color = Color.Black)
    ) {
        Column (
            modifier
                .fillMaxSize()
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            //Data CO, CO2, PM2.5
            Row(
                modifier
                    .fillMaxWidth(),
                    //.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Sensor("CO",
                    "$co ppm",
                    painterResource(id = R.drawable.co),
                    modifier
                    //.fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    textColor = coColor)
                Sensor("CO₂", "$co2 ppm",painterResource(id = R.drawable.co2), modifier
                    //.fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    textColor = co2Color)
            }
            //Data suhu & kelembapan
            Row (
                modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Sensor("Kelembapan", "${kelembapan}%", painterResource(id = R.drawable.humidity),  modifier
                    //.fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    textColor = kelembapanColor)
                Sensor("Suhu", "${suhu}°C",painterResource(id = R.drawable.celcius), modifier
                    //.fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    textColor = suhuColor)
            }
            Column(
                modifier
                    .fillMaxWidth(),
                    //.border(2.dp, Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Sensor(
                    "PM2.5", "$pm25 µg/m³", painterResource(id = R.drawable.airpollution), modifier
                        .padding(top = 12.dp, bottom = 12.dp, end = 12.dp),
                    textColor = pm25Color
                )
            }
            Text(
                text = keterangan,
                fontSize = 20.sp,
                color = Color(0xFF00695C)
            )
        }
    }
}

@Composable
fun DataHistory(data: AirQualityData, modifier: Modifier = Modifier) {
    val dateFormatted = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(data.tanggal)
    val coFortmated = String.format(Locale.US, "%.2f", data.co)
    val co2Fortmated = String.format(Locale.US, "%.2f", data.co2)

    Surface(
        modifier
            .fillMaxWidth()
            .heightIn(170.dp)
            .border(2.dp, Color(0xFF00B894), RoundedCornerShape(16.dp)),
        color = Color(0xFFE0F7FA),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier
                .fillMaxSize()
                .padding(5.dp)
        ) {
            Text(
                dateFormatted,
                fontSize = 19.sp,
                color = Color(0xFF2D3436)
            )
            Row(
                modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                Spacer(modifier.height(18.dp))
                Sensor("CO", "$coFortmated ppm", painterResource(id = R.drawable.co), modifier
                    .weight(1f)
                    .padding(start = 1.dp), fontSize = 14.sp)
                Sensor("Kelembapan", "${data.kelembapan} %", painterResource(id = R.drawable.humidity),  modifier
                    .weight(1f)
                    .padding(start = 1.dp), fontSize = 14.sp)
            }
            Row(
                modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                Sensor("CO2", "$co2Fortmated ppm", painterResource(id = R.drawable.co2), modifier
                    .weight(1f)
                    .padding(start = 1.dp), fontSize = 14.sp)
                Sensor("PM2.5", "${data.pm2_5} µg/m³", painterResource(id = R.drawable.airpollution), modifier
                    .weight(1f)
                    .padding(start = 1.dp), fontSize = 14.sp)
                Sensor("Suhu", " ${data.suhu} °C", painterResource(id = R.drawable.celcius), modifier
                    .weight(1f)
                    .padding(start = 1.dp), fontSize = 14.sp)
            }
            Column (
                modifier
                    .weight(1f)
            ) {
                Box(
                    modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFF00B894))
                )
                Column(
                    modifier
                        .fillMaxSize()
                        .weight(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(data.keterangan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SetLimitDialog(
    context: Context,
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSendSet: (Map<String, Any>) -> Unit
) {
    var co by remember { mutableStateOf("") }
    var co2 by remember { mutableStateOf("") }
    var suhu by remember { mutableStateOf("") }
    var kelembapan by remember { mutableStateOf("") }
    var pm25 by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set Limit Index AQI Ideal") },
            text = {
                Column {
                    Text("Masukan Batas Index AQI Terbaru")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = co,
                        onValueChange = { co = it },
                        label = { Text("CO") },
                        placeholder = { Text("Enter Limit CO") },
                        trailingIcon = {
                            Text("PPM")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = co2,
                        onValueChange = { co2 = it },
                        label = { Text("CO2") },
                        placeholder = { Text("Enter Limit CO2") },
                        trailingIcon = {
                            Text("PPM")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = suhu,
                        onValueChange = { suhu = it },
                        label = { Text("Suhu") },
                        placeholder = { Text("Enter Limit Suhu") },
                        trailingIcon = {
                            Text("°C")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kelembapan,
                        onValueChange = { kelembapan = it },
                        label = { Text("Kelembapan") },
                        placeholder = { Text("Enter Limit Kelembapan") },
                        trailingIcon = {
                            Text("%")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pm25,
                        onValueChange = { pm25 = it },
                        label = { Text("PM2.5") },
                        placeholder = { Text("Enter Limit PM2.5") },
                        trailingIcon = {
                            Text("µg/m³")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (co.isBlank() || co2.isBlank() || suhu.isBlank() || kelembapan.isBlank() || pm25.isBlank()) {
                            Log.d("SetLimitDialog", "Input tidak boleh kosong!")
                            Toast.makeText(context, "Kode Akses harus diisi!", Toast.LENGTH_SHORT).show()
                            return@TextButton // batalkan kirim kalau ada yang kosong
                        }
                        val data: Map<String, Any> = mapOf(
                            "coLimit" to (co.toFloatOrNull() ?: 0f),
                            "co2Limit" to (co2.toFloatOrNull() ?: 0f),
                            "suhuLimit" to (suhu.toLongOrNull() ?: 0f),
                            "kelembapanLimit" to (kelembapan.toLongOrNull() ?: 0f),
                            "pm2_5Limit" to (pm25.toLongOrNull() ?: 0f)
                        )
                        onSendSet(data)
                        onDismiss()
                    }
                ) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ConfirmReset(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSendSet: (Map<String, Any>) -> Unit
) {
    if(showDialog){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {Text("Konfirmasi Reset Limit")},
            text = {Text("Apakah Anda Yakin Ingin Mereset Limit Sesuai Bawaan Sistem?")},
            confirmButton = {
                TextButton(
                    onClick = {
                        onSendSet(defaultLimits)
                        onDismiss()
                    }
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ConfirmLogout(
    showDialog: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("L O G O U T")},
            text = { Text("Apakah Kamu Yakin Ingin Logout?")},
            confirmButton = {
                TextButton(
                    onClick = {onConfirm()}
                ) {
                    Text("Ya")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {onDismiss()}
                ) {
                    Text("Batal")
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        )
    }
}

@Composable
fun Information(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Informasi")},
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    InfoRow("ppm","Parts per million, digunakan untuk mengukur konsentrasi gas CO dan CO₂.")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("µg/m³","Mikrogram per meter kubik, satuan untuk mengukur partikel udara seperti PM2.5.")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("%","Persen, digunakan untuk kelembapan relatif (humidity).")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("°C","Derajat Celcius, satuan suhu.")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("CO","Carbon Monoxide / Karbon monoksida, gas yang dihasilkan dari pembakaran yang tidak sempurna.")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("CO₂","Carbon Dioxide / Karbon Dioksida, gas yang di hasilkan dari pernapasan manusia, pembakaran bahan bakar, dan lain sebagainya.")
                    Spacer(modifier = Modifier.size(5.dp))
                    InfoRow("PM2.5","Particulate Matter ≤ 2.5 µm, partikel debu di udara dengan diameter ≤ 2.5 mikrometer")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {onDismiss()}
                ) {
                    Text("Tutup")
                }
            }
            //properties = DialogProperties(dismissOnClickOutside = false)
        )
    }
}

@Composable
fun InfoRow(label: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(60.dp), // Atur lebar tetap agar rata
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = ":",
            modifier = Modifier.width(10.dp), // Kolom khusus untuk titik dua
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = description,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )
        Spacer(modifier = Modifier.size(5.dp))
    }
}