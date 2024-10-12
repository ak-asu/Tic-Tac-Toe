package com.akheparasu.tic_tac_toe.screens

import android.os.Bundle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akheparasu.tic_tac_toe.utils.GameMode
import com.akheparasu.tic_tac_toe.utils.LocalNavController


@Composable
fun HomeScreen() {
    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { navController?.navigate("game/${GameMode.Computer.name}") }) {
            Text(text = "Play against Computer")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController?.navigate("game/${GameMode.Human.name}") }) {
            Text(text = "Play against Player")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController?.navigate("game/${GameMode.Online.name}") }) {
            Text(text = "Play Online")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController?.navigate("career") }) {
            Text(text = "Career")
        }
    }
}
