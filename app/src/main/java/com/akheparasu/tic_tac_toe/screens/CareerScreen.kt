package com.akheparasu.tic_tac_toe.screens

import android.app.Application
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akheparasu.tic_tac_toe.storage.DataEntity
import com.akheparasu.tic_tac_toe.storage.StorageDB
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CareerScreen(careerViewModel: CareerViewModel) {
    val records by careerViewModel.records.collectAsState()
    val headers = listOf("Date", "Winner", "Difficulty")

    LaunchedEffect(Unit) {
        careerViewModel.getAllRecords()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Gray),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(headers.size) { index ->
                    Text(
                        headers[index],
                        modifier = Modifier
                            .weight(1f)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        items(records.size) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val values = listOf(
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(records[index].date),
                    records[index].winner.getDisplayText(),
                    records[index].difficulty?.name ?: records[index].gameMode.getDisplayText()
                )
                repeat(values.size) { i ->
                    Text(
                        values[i],
                        modifier = Modifier
                            .weight(1f)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface)),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

class CareerViewModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = StorageDB.getDatabase(application).dataDao()

    private val _records = MutableStateFlow<List<DataEntity>>(emptyList())
    val records: StateFlow<List<DataEntity>> get() = _records
    fun getAllRecords() {
        viewModelScope.launch {
            dataDao.getAllRows().collect { data -> _records.value = data }
        }
    }
}

class CareerViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CareerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CareerViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun DisplayText(
    key: String,
    value: String? = null
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(key)
            }
            if (value != null) {
                append(value)
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
