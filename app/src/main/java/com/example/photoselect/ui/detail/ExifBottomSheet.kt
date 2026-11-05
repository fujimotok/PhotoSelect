package com.example.photoselect.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Panorama
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.ShutterSpeed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photoselect.data.exif.ExifMetadata
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.components.BadgeChip
import java.util.Locale

/**
 * EXIF メタデータおよびファイル詳細を表示するボトムシートコンポーザブル
 *
 * @param mediaGroup 対象の [MediaGroup]
 * @param exif 解析済みの [ExifMetadata]
 * @param sheetState ボトムシートの状態管理 [SheetState]
 * @param onDismiss 閉じる操作時のコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifBottomSheet(
    mediaGroup: MediaGroup,
    exif: ExifMetadata,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mediaGroup.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (mediaGroup.hasRaw) {
                        BadgeChip(
                            text = "RAW",
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                    if (mediaGroup.isVideo) {
                        BadgeChip(
                            text = "VIDEO",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }
            }

            HorizontalDivider()

            // カメラ本体
            ExifInfoRow(
                icon = Icons.Default.CameraAlt,
                label = "Camera",
                value = exif.cameraDisplay
            )

            // レンズモデル
            if (!exif.lensModel.isNullOrEmpty()) {
                ExifInfoRow(
                    icon = Icons.Default.Camera,
                    label = "Lens",
                    value = exif.lensModel
                )
            }

            // 露出パラメータ (シャッタースピード, 絞り, ISO, 焦点距離)
            ExifInfoRow(
                icon = Icons.Default.ShutterSpeed,
                label = "Exposure",
                value = exif.exposureDisplay
            )

            // 画像解像度
            ExifInfoRow(
                icon = Icons.Default.Panorama,
                label = "Dimensions",
                value = exif.resolutionDisplay
            )

            // 撮影日時
            if (!exif.dateTime.isNullOrEmpty()) {
                ExifInfoRow(
                    icon = Icons.Default.DateRange,
                    label = "Date Taken",
                    value = exif.dateTime
                )
            }

            // 代表ファイル情報（サイズ・拡張子）
            val primaryFile = mediaGroup.primaryFile ?: mediaGroup.videoFile ?: mediaGroup.rawFile
            if (primaryFile != null) {
                val sizeMb = String.format(Locale.US, "%.2f MB", primaryFile.size / (1024.0 * 1024.0))
                ExifInfoRow(
                    icon = Icons.Default.SdCard,
                    label = "File Size",
                    value = "$sizeMb (${primaryFile.extension.uppercase()})"
                )
            }

            // ペアリングRAWファイル情報
            if (mediaGroup.hasRaw && mediaGroup.rawFile != null) {
                val rawSizeMb = String.format(Locale.US, "%.2f MB", mediaGroup.rawFile.size / (1024.0 * 1024.0))
                ExifInfoRow(
                    icon = Icons.Default.Info,
                    label = "Paired RAW",
                    value = "${mediaGroup.rawFile.name} ($rawSizeMb)"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * EXIF 情報の各行を表示するコンポーザブル
 */
@Composable
fun ExifInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

