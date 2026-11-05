package com.example.photoselect.ui.comparison

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.fujimotok.photoselect.R
import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.components.BadgeChip

/**
 * 比較画面の分割表示レイアウト（自動、左右分割、上下分割）
 */
enum class SplitOrientation {
    /** 画面の回転向きに応じて自動切り替え */
    AUTO,
    /** 左右（横並び）分割 */
    HORIZONTAL,
    /** 上下（縦並び）分割 */
    VERTICAL
}

/**
 * 2枚の写真を並列表示し、完全同期したパン＆ズーム操作でピンボケ・微小な表情差を比較する画面
 *
 * @param mediaGroups 比較対象のメディアグループ全リスト
 * @param baseIndex 基準画像（固定表示側）のインデックス
 * @param onNavigateBack 比較画面の終了コールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    mediaGroups: List<MediaGroup>,
    baseIndex: Int,
    onNavigateBack: () -> Unit,
) {
    if (mediaGroups.isEmpty()) {
        onNavigateBack()
        return
    }

    val safeBaseIndex = baseIndex.coerceIn(0, mediaGroups.size - 1)
    val baseGroup = mediaGroups[safeBaseIndex]

    // 比較対象（スワイプ切り替え可能側）の初期ページ設定
    val initialTargetIndex = if (safeBaseIndex + 1 < mediaGroups.size) {
        safeBaseIndex + 1
    } else if (safeBaseIndex - 1 >= 0) {
        safeBaseIndex - 1
    } else {
        safeBaseIndex
    }

    val comparePagerState = rememberPagerState(
        initialPage = initialTargetIndex,
        pageCount = { mediaGroups.size }
    )

    val configuration = LocalConfiguration.current

    // 2枚の画面で完全同期させる拡大率・パン移動量のステート
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // ユーザーによるレイアウト分割方法の切り替えオプション
    var splitOrientation by remember { mutableStateOf(SplitOrientation.AUTO) }

    val isHorizontalSplit = when (splitOrientation) {
        SplitOrientation.AUTO -> configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        SplitOrientation.HORIZONTAL -> true
        SplitOrientation.VERTICAL -> false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Photo Comparison Mode",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Zoom: ${(scale * 100).toInt()}% | Swipe second pane to change photo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit Comparison"
                        )
                    }
                },
                actions = {
                    // 分割方向切り替えボタン
                    IconButton(
                        onClick = {
                            splitOrientation = when (splitOrientation) {
                                SplitOrientation.AUTO -> SplitOrientation.HORIZONTAL
                                SplitOrientation.HORIZONTAL -> SplitOrientation.VERTICAL
                                SplitOrientation.VERTICAL -> SplitOrientation.AUTO
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (splitOrientation) {
                                SplitOrientation.AUTO -> Icons.Default.Splitscreen
                                SplitOrientation.HORIZONTAL -> Icons.Default.ViewColumn
                                SplitOrientation.VERTICAL -> Icons.Default.ViewAgenda
                            },
                            contentDescription = "Toggle Split Layout"
                        )
                    }

                    // ズームリセットボタン
                    IconButton(
                        onClick = {
                            scale = 1f
                            offset = Offset.Zero
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Zoom"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // ダブルタップで全画面・等倍と拡大表示をトグル
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    scale = 1f
                                    offset = Offset.Zero
                                } else {
                                    scale = 2.5f
                                    offset = Offset.Zero
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        // ピンチイン・アウトおよびドラッグ操作を同期検出
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 10f)
                            scale = newScale
                            if (newScale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
            ) {
                // 基準画像の描画領域
                val basePaneContent = @Composable { modifier: Modifier ->
                    ComparisonImagePane(
                        tagLabel = "BASE (${safeBaseIndex + 1}/${mediaGroups.size})",
                        mediaGroup = baseGroup,
                        scale = scale,
                        offset = offset,
                        modifier = modifier
                    )
                }

                // スワイプ可能な比較対象画像の描画領域（HorizontalPager）
                val targetPaneContent = @Composable { modifier: Modifier ->
                    HorizontalPager(
                        state = comparePagerState,
                        modifier = modifier
                    ) { page ->
                        ComparisonImagePane(
                            tagLabel = "TARGET (${page + 1}/${mediaGroups.size})",
                            mediaGroup = mediaGroups[page],
                            scale = scale,
                            offset = offset,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (isHorizontalSplit) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        basePaneContent(Modifier.weight(1f).fillMaxSize())
                        targetPaneContent(Modifier.weight(1f).fillMaxSize())
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        basePaneContent(Modifier.weight(1f).fillMaxSize())
                        targetPaneContent(Modifier.weight(1f).fillMaxSize())
                    }
                }
            }

            // 拡大時のみ表示するズーム調整コントローラーオーバーレイ
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (scale > 1.05f) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(24.dp),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                    scale = newScale
                                    if (newScale == 1f) offset = Offset.Zero
                                }
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                            }

                            Text(
                                text = "${(scale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = {
                                    scale = (scale + 0.5f).coerceAtMost(10f)
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In")
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitScreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 比較画面内の単一画像パネル（基準/比較対象）
 */
@Composable
fun ComparisonImagePane(
    tagLabel: String,
    mediaGroup: MediaGroup,
    scale: Float,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clipToBounds()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaGroup.displayUri)
                .crossfade(true)
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.placeholder)
                .build(),
            contentDescription = mediaGroup.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        // 上部のファイル名およびタグ（BASE / TARGET）表示オーバーレイ
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tagLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = mediaGroup.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (mediaGroup.hasRaw) {
                    BadgeChip(
                        text = "RAW",
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComparisonScreenPreview() {
    val group1 = MediaGroup(
        baseName = "DSC0001",
        primaryFile = MediaFile(name = "DSC0001.JPG", extension = "JPG", size = 1000L, lastModified = 1000L),
        rawFile = MediaFile(name = "DSC0001.RW2", extension = "RW2", size = 5000L, lastModified = 1000L)
    )
    val group2 = MediaGroup(
        baseName = "DSC0002",
        primaryFile = MediaFile(name = "DSC0002.JPG", extension = "JPG", size = 1000L, lastModified = 1000L)
    )
    ComparisonScreen(
        mediaGroups = listOf(group1, group2),
        baseIndex = 0,
        onNavigateBack = {}
    )
}

