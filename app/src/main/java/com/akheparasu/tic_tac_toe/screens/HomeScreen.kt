package com.akheparasu.tic_tac_toe.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.LocalConnectionService
import com.akheparasu.tic_tac_toe.utils.LocalNavController

@Composable
fun HomeScreen() {
    val navController = LocalNavController.current
    val connectionService = LocalConnectionService.current
    val context = LocalContext.current
    val connectionState by connectionService?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { navController?.navigate("game") }) {
            Text(text = "Play against Computer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController?.navigate("game") }) {
            Text(text = "Play against Player")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            if (connectionState) {
                navController?.navigate("game")
            } else {
                Toast.makeText(context, "No Bluetooth connection", Toast.LENGTH_SHORT).show()
            }
        }, enabled = connectionState) {
            Text(text = "Play Online")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController?.navigate("career") }) {
            Text(text = "Career")
        }
    }
}
