package com.akheparasu.tic_tac_toe.screens

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun CareerScreen(careerViewModel: CareerViewModel) {
    val records by careerViewModel.records.collectAsState()

    LaunchedEffect(Unit) {
        careerViewModel.getAllRecords()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(records.size) { record ->
            Card {
                DisplayText("Date: ", records[record].date.toString())
                DisplayText("Winner: ", records[record].winner.toString())
                DisplayText("Difficulty: ", records[record].difficulty?.name ?: "-")
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
