package io.github.jpcottin.excusemyfrench.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.jpcottin.excusemyfrench.data.remote.InsultApiServiceImpl
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepository
import io.github.jpcottin.excusemyfrench.data.repository.InsultRepositoryImpl
import io.github.jpcottin.excusemyfrench.data.settings.DataStoreSettingsRepository
import io.github.jpcottin.excusemyfrench.data.settings.SettingsRepository

class InsultViewModelFactory(
    private val application: Application,
    private val repository: InsultRepository = InsultRepositoryImpl(InsultApiServiceImpl()),
    private val settings: SettingsRepository = DataStoreSettingsRepository(application)
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsultViewModel::class.java)) {
            val ttsService = io.github.jpcottin.excusemyfrench.service.TtsServiceImpl(application)
            @Suppress("UNCHECKED_CAST")
            return InsultViewModel(application, repository, ttsService, settings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
