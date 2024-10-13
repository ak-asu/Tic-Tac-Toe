package com.akheparasu.tic_tac_toe.multiplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DevicesDialog(onYes: () -> Unit, onNo: () -> Unit) {
//    AlertDialog(
//        onDismissRequest = { /* Do nothing on outside touch */ },
//        title = { Text("Are you ready?") },
//        text = { Text("Do you want to start the game now?") },
//        buttons = {
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(all = 8.dp),
//                horizontalArrangement = Arrangement.SpaceAround
//            ) {
//                // No button
//                Button(
//                    onClick = onNo,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text("No")
//                }
//
//                Spacer(modifier = Modifier.width(8.dp))
//
//                // Yes button
//                Button(
//                    onClick = onYes,
//                    modifier = Modifier.weight(1f)
//                ) {
//                    Text("Yes")
//                }
//            }
//        }
//     )
}
