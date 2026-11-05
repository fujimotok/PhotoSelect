package com.example.photoselect.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.comparison.ComparisonScreen
import com.example.photoselect.ui.detail.DetailScreen
import com.example.photoselect.ui.gallery.GalleryScreen
import com.example.photoselect.ui.help.HelpScreen
import com.example.photoselect.ui.settings.SettingsScreen

/**
 * アプリケーション内の画面（スクリーン）を表すインターフェース
 */
sealed interface Screen {
    /** [SaveableStateProvider] で状態を識別・保持するためのキー */
    val key: String

    /** ギャラリー一覧画面 */
    data object Gallery : Screen {
        override val key: String = "Gallery"
    }

    /** 拡張子設定画面 */
    data object Settings : Screen {
        override val key: String = "Settings"
    }

    /** ヘルプ＆アプリ情報画面 */
    data object Help : Screen {
        override val key: String = "Help"
    }

    /** 2画面比較画面 */
    data class Comparison(val mediaGroups: List<MediaGroup>, val baseIndex: Int) : Screen {
        override val key: String = "Comparison_${baseIndex}_${mediaGroups.hashCode()}"
    }

    /** 詳細プレビュー画面 */
    data class Detail(val mediaGroups: List<MediaGroup>, val initialIndex: Int) : Screen {
        override val key: String = "Detail_${initialIndex}_${mediaGroups.hashCode()}"
    }
}

/**
 * ルーティングおよびバックスタック管理を担当するメイン画面コンポーザブル
 */
@Composable
fun MainScreen() {
    // 画面遷移のバックスタック管理用ステート
    var backStack by remember { mutableStateOf<List<Screen>>(listOf(Screen.Gallery)) }
    val saveableStateHolder = rememberSaveableStateHolder()

    val currentScreen = backStack.lastOrNull() ?: Screen.Gallery

    // システム戻るジェスチャー／ボタンのハンドリング
    BackHandler(enabled = backStack.size > 1) {
        backStack = backStack.dropLast(1)
    }

    // 画面ごとのUI状態（スクロール位置等）を保持
    saveableStateHolder.SaveableStateProvider(key = currentScreen.key) {
        when (currentScreen) {
            is Screen.Gallery -> {
                GalleryScreen(
                    onNavigateToSettings = {
                        backStack = backStack + Screen.Settings
                    },
                    onNavigateToHelp = {
                        backStack = backStack + Screen.Help
                    },
                    onNavigateToDetail = { groups, initialIndex ->
                        backStack = backStack + Screen.Detail(groups, initialIndex)
                    }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    onNavigateBack = {
                        if (backStack.size > 1) {
                            backStack = backStack.dropLast(1)
                        } else {
                            backStack = listOf(Screen.Gallery)
                        }
                    }
                )
            }
            is Screen.Help -> {
                HelpScreen(
                    onNavigateBack = {
                        if (backStack.size > 1) {
                            backStack = backStack.dropLast(1)
                        } else {
                            backStack = listOf(Screen.Gallery)
                        }
                    }
                )
            }
            is Screen.Comparison -> {
                ComparisonScreen(
                    mediaGroups = currentScreen.mediaGroups,
                    baseIndex = currentScreen.baseIndex,
                    onNavigateBack = {
                        if (backStack.size > 1) {
                            backStack = backStack.dropLast(1)
                        } else {
                            backStack = listOf(Screen.Gallery)
                        }
                    }
                )
            }
            is Screen.Detail -> {
                DetailScreen(
                    mediaGroups = currentScreen.mediaGroups,
                    initialIndex = currentScreen.initialIndex,
                    onNavigateBack = {
                        if (backStack.size > 1) {
                            backStack = backStack.dropLast(1)
                        } else {
                            backStack = listOf(Screen.Gallery)
                        }
                    },
                    onNavigateToComparison = { groups, baseIndex ->
                        backStack = backStack + Screen.Comparison(groups, baseIndex)
                    }
                )
            }
        }
    }
}

