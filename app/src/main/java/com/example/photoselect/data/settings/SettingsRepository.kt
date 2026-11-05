package com.example.photoselect.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Jetpack DataStore (Preferences) を用いた設定保持リポジトリ
 *
 * ユーザーがカスタマイズしたプライマリ画像拡張子、RAW画像拡張子、選択されたTree URIなどを非同期Flowで提供します。
 */
class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_PRIMARY_EXTENSIONS = stringPreferencesKey("primary_extensions")
        val KEY_RAW_EXTENSIONS = stringPreferencesKey("raw_extensions")
        val KEY_TREE_URI = stringPreferencesKey("tree_uri")

        /** デフォルトのプライマリ画像拡張子セット */
        val DEFAULT_PRIMARY_EXTENSIONS = setOf("JPG", "JPEG", "HIF", "HEIC")
        /** デフォルトのRAW画像拡張子セット */
        val DEFAULT_RAW_EXTENSIONS = setOf("RW2", "ARW", "CR3", "NEF", "RAF", "ORF", "DNG")
    }

    /** プライマリ画像拡張子セットのFlow */
    val primaryExtensionsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val csv = preferences[KEY_PRIMARY_EXTENSIONS]
            if (csv.isNullOrBlank()) {
                DEFAULT_PRIMARY_EXTENSIONS
            } else {
                parseCsvToExtensions(csv)
            }
        }

    /** RAW画像拡張子セットのFlow */
    val rawExtensionsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val csv = preferences[KEY_RAW_EXTENSIONS]
            if (csv.isNullOrBlank()) {
                DEFAULT_RAW_EXTENSIONS
            } else {
                parseCsvToExtensions(csv)
            }
        }

    /** 現在選択されているTree URI文字列のFlow */
    val treeUriFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_TREE_URI]
        }

    /** プライマリ画像拡張子（カンマ区切り文字列）を保存 */
    suspend fun savePrimaryExtensions(extensionsCsv: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRIMARY_EXTENSIONS] = extensionsCsv
        }
    }

    /** RAW画像拡張子（カンマ区切り文字列）を保存 */
    suspend fun saveRawExtensions(extensionsCsv: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_RAW_EXTENSIONS] = extensionsCsv
        }
    }

    /** 選択フォルダの Tree URI 文字列を保存 */
    suspend fun saveTreeUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[KEY_TREE_URI] = uriString
            } else {
                preferences.remove(KEY_TREE_URI)
            }
        }
    }

    /** 拡張子設定をデフォルトに戻す */
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PRIMARY_EXTENSIONS)
            preferences.remove(KEY_RAW_EXTENSIONS)
        }
    }

    /** カンマ区切りの文字列をトリム・大文字化して Set に変換 */
    private fun parseCsvToExtensions(csv: String): Set<String> {
        return csv.split(",")
            .map { it.trim().uppercase().removePrefix(".") }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
