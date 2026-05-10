package com.example.aqiapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aqiapp.page.HistoryPage
import com.example.aqiapp.page.LoginPage
import com.example.aqiapp.page.HomePage
import com.example.aqiapp.page.SettingPage
import com.google.firebase.database.FirebaseDatabase

@Composable
fun NavApps(modifier: Modifier = Modifier) {
    //BUat NavController
    val navController = rememberNavController()
    var nomorRuangan by rememberSaveable { mutableStateOf("") }
    val database = FirebaseDatabase.getInstance("https://aqi-00-138ef-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    //Buat NavHost
    NavHost(navController = navController, startDestination = "login"){
        composable ("login") {
            LoginPage(
                modifier,
                onLoginSucess = {ruangan ->
                    nomorRuangan = ruangan
                    navController.navigate("home"){
                        popUpTo("login") { inclusive = true }  // menghapus "login" dari backstack
                    }

                }

            )
        }
        composable ("home") {
            HomePage (
                { route -> navController.navigate(route)},
                database = database,
                nomorRuangan
            )
        }
        composable ("history") {
            HistoryPage({ route -> navController.navigate(route)}, modifier, nomorRuangan = nomorRuangan)
        }
        composable ("setting") {
            SettingPage(navController,{ route -> navController.navigate(route)}, modifier, nomorRuangan = nomorRuangan)
        }
    }

}