package com.example.photoselect.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoselect.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [SettingsScreen] のUI状態および設定保存処理を管理する ViewModel
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    /** 現在設定されているプライマリ画像拡張子セットの StateFlow */
    val primaryExtensions: StateFlow<Set<String>> = settingsRepository.primaryExtensionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_PRIMARY_EXTENSIONS)

    /** 現在設定されているRAW画像拡張子セットの StateFlow */
    val rawExtensions: StateFlow<Set<String>> = settingsRepository.rawExtensionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.DEFAULT_RAW_EXTENSIONS)

    /**
     * 入力されたカンマ区切り文字列をパースし、設定リポジトリに保存します。
     *
     * @param primaryCsv プライマリ拡張子カンマ区切り文字列
     * @param rawCsv RAW拡張子カンマ区切り文字列
     */
    fun saveExtensions(primaryCsv: String, rawCsv: String) {
        viewModelScope.launch {
            settingsRepository.savePrimaryExtensions(primaryCsv)
            settingsRepository.saveRawExtensions(rawCsv)
        }
    }

    /**
     * 拡張子設定をデフォルト初期値にリセットします。
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
}

