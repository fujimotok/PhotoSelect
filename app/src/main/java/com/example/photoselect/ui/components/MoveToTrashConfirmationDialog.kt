package com.example.photoselect.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * ファイルのゴミ箱移動確認ダイアログ
 *
 * @param title ダイアログタイトル（デフォルト: "Move to Trash"）
 * @param message 確認メッセージ（削除件数やペアリングファイルについての注意書き等）
 * @param onConfirm 確定ボタン押下時の処理
 * @param onDismiss 削除キャンセルまたは領域外タップ時の処理
 */
@Composable
fun MoveToTrashConfirmationDialog(
    title: String = "Move to Trash",
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Move to Trash",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

