package com.example.aqiapp.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aqiapp.reusable.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

@Composable
fun HistoryPage(
    navigateTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    nomorRuangan: String
) {
    val firestore = Firebase.firestore
    var historyList by remember { mutableStateOf<List<AirQualityData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(nomorRuangan) {
        firestore.collection("riwayatKualitasUdara")
            .document(nomorRuangan)
            .collection("harian")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                snapshot?.let {
                    historyList = it.documents.mapNotNull { doc ->
                        doc.toObject(AirQualityData::class.java)
                    }
                    isLoading = false
                }
            }
    }

    Scaffold(
        modifier
            .navigationBarsPadding(),
        bottomBar = {
            AppBottomBar(currentRoute = "history", onItemSelected = navigateTo)
        },
        containerColor = Color(0xFFE9F5F2)
    ) {padding ->

            Column(
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text("Riwayat Kualitas Udara",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                //List
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                    color = Color.Transparent,
                    //shadowElevation = 4.dp,
                ){
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        LazyColumn(modifier.padding(top = 1.dp)) {
                            items(historyList) { item ->
                                DataHistory(data = item) // satu per satu, atau ubah komposisi UI nanti
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

    }
}