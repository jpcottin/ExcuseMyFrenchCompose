package com.example.excusemyfrenchcompose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.excusemyfrenchcompose.data.remote.InsultApiServiceImpl
import com.example.excusemyfrenchcompose.data.repository.InsultRepository
import com.example.excusemyfrenchcompose.data.repository.InsultRepositoryImpl
import com.example.excusemyfrenchcompose.data.settings.DataStoreSettingsRepository
import com.example.excusemyfrenchcompose.data.settings.SettingsRepository

class InsultViewModelFactory(
    private val application: Application,
    private val repository: InsultRepository = InsultRepositoryImpl(InsultApiServiceImpl()),
    private val settings: SettingsRepository = DataStoreSettingsRepository(application)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsultViewModel::class.java)) {
            val ttsService = com.example.excusemyfrenchcompose.service.TtsServiceImpl(application)
            @Suppress("UNCHECKED_CAST")
            return InsultViewModel(application, repository, ttsService, settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
