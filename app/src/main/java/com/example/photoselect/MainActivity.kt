package com.example.photoselect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.photoselect.ui.MainScreen
import com.example.photoselect.ui.theme.PhotoSelectTheme

/**
 * Photo Select アプリケーションのメインアクティビティ
 * 
 * Jetpack Compose を利用した単一アクティビティ（Single Activity）構成のエントリーポイントです。
 * エッジ・トゥ・エッジ描画を有効化し、[MainScreen] をルートビューとして呼び出します。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // システムバー（ステータスバー・ナビゲーションバー）領域までのエッジ・トゥ・エッジ表示を有効化
        enableEdgeToEdge()
        setContent {
            PhotoSelectTheme {
                MainScreen()
            }
        }
    }
}
