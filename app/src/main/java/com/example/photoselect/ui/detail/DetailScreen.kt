package com.example.photoselect.ui.detail

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.fujimotok.photoselect.R
import com.example.photoselect.data.exif.ExifMetadata
import com.example.photoselect.data.exif.ExifReader
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.components.BadgeChip
import com.example.photoselect.ui.components.MoveToTrashConfirmationDialog
import com.example.photoselect.ui.gallery.GalleryViewModel
import com.example.photoselect.util.FileShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * 単一メディアのフルスクリーン詳細表示画面
 *
 * スワイプによる前後ファイル移動（HorizontalPager）、ピンチイン/アウトでの拡大・パン、
 * EXIF情報の確認、2画面比較モードへの遷移、個別共有およびゴミ箱移動に対応しています。
 *
 * @param mediaGroups 表示対象のメディアグループ一覧
 * @param initialIndex 初期表示インデックス
 * @param onNavigateBack 画面終了時の戻り遷移コールバック
 * @param onNavigateToComparison 比較画面への遷移コールバック
 * @param viewModel 削除等のデータ操作を実行する ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    mediaGroups: List<MediaGroup>,
    initialIndex: Int,
    onNavigateBack: () -> Unit,
    onNavigateToComparison: (List<MediaGroup>, Int) -> Unit,
    viewModel: GalleryViewModel = viewModel()
) {
    var displayedGroups by remember(mediaGroups) { mutableStateOf(mediaGroups) }

    if (displayedGroups.isEmpty()) {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val safeInitialIndex = initialIndex.coerceIn(0, displayedGroups.size - 1)
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { displayedGroups.size }
    )

    LaunchedEffect(safeInitialIndex) {
        if (pagerState.currentPage != safeInitialIndex) {
            pagerState.scrollToPage(safeInitialIndex)
        }
    }

    val safeCurrentPage = pagerState.currentPage.coerceIn(0, (displayedGroups.size - 1).coerceAtLeast(0))
    val currentGroup = displayedGroups.getOrNull(safeCurrentPage) ?: run {
        LaunchedEffect(Unit) {
            onNavigateBack()
        }
        return
    }

    var exifMetadata by remember { mutableStateOf(ExifMetadata()) }
    var showExifSheet by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showBars by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 現在選択中のファイルに応じた EXIF 情報のバックグラウンド非同期読み込み
    LaunchedEffect(currentGroup) {
        withContext(Dispatchers.IO) {
            exifMetadata = ExifReader.readExif(context, currentGroup)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // フルスクリーン・横スワイプ対応 HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val group = displayedGroups.getOrNull(page) ?: return@HorizontalPager
            if (group.isVideo) {
                val videoFile = group.videoFile ?: group.primaryFile
                val videoUri = videoFile?.uri
                val fileName = videoFile?.name ?: "Video"
                var thumbnailBitmap by remember(videoUri) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(videoUri) {
                    if (videoUri != null) {
                        withContext(Dispatchers.IO) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(context, videoUri)
                                thumbnailBitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            } catch (e: Exception) {
                                Log.e("DetailScreen", "Failed to retrieve video thumbnail", e)
                            } finally {
                                try {
                                    retriever.release()
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(videoUri) {
                            detectTapGestures(
                                onTap = { showBars = !showBars }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🎬 Video File",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = fileName,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                if (videoUri != null) {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(videoUri, "video/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("DetailScreen", "Failed to open video in external player", e)
                                    }
                                }
                            }
                        ) {
                            Text("▶ Play in External Player")
                        }
                    }
                }
            } else {
                ZoomablePhotoView(
                    mediaGroup = group,
                    onTap = { showBars = !showBars },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 上部コントロールバー（半透明オーバーレイ）
        if (showBars) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.2f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = currentGroup.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${safeCurrentPage + 1} of ${displayedGroups.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // 下部アクションバー（EXIF, 比較, 削除, 共有）
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.2f),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // EXIF 露出情報表示および詳細ボトムシートトリガー
                    Row(
                        modifier = Modifier.clickable(onClick = { showExifSheet = true }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "EXIF Info",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (exifMetadata.exposureDisplay.isNotEmpty()) {
                            Text(
                                text = exifMetadata.exposureDisplay,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (currentGroup.hasRaw) {
                            BadgeChip(text = "RAW", containerColor = MaterialTheme.colorScheme.primaryContainer)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (currentGroup.isVideo) {
                            BadgeChip(text = "VIDEO", containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        }
                    }

                    // アクションボタン群
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 2画面比較モード起動
                        IconButton(onClick = { onNavigateToComparison(displayedGroups, safeCurrentPage) }) {
                            Icon(
                                imageVector = Icons.Default.Compare,
                                contentDescription = "Compare",
                                tint = Color.White
                            )
                        }
                        // ゴミ箱移動
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                        // ファイル共有（RAW/JPEG選択ドロップダウン対応）
                        Box {
                            IconButton(onClick = {
                                if (currentGroup.hasRaw && currentGroup.primaryFile != null) {
                                    showShareMenu = true
                                } else {
                                    val target = currentGroup.primaryFile ?: currentGroup.videoFile ?: currentGroup.rawFile
                                    target?.let { file ->
                                        coroutineScope.launch {
                                            FileShareHelper.shareMediaFile(context, file)
                                        }
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share File",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                currentGroup.primaryFile?.let { primary ->
                                    DropdownMenuItem(
                                        text = { Text("Share Primary JPEG (${primary.extension})") },
                                        onClick = {
                                            showShareMenu = false
                                            coroutineScope.launch {
                                                FileShareHelper.shareMediaFile(context, primary)
                                            }
                                        }
                                    )
                                }
                                currentGroup.rawFile?.let { raw ->
                                    DropdownMenuItem(
                                        text = { Text("Share Paired RAW (${raw.extension})") },
                                        onClick = {
                                            showShareMenu = false
                                            coroutineScope.launch {
                                                FileShareHelper.shareMediaFile(context, raw)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showExifSheet) {
            ExifBottomSheet(
                mediaGroup = currentGroup,
                exif = exifMetadata,
                sheetState = sheetState,
                onDismiss = { showExifSheet = false }
            )
        }

        if (showDeleteDialog) {
            MoveToTrashConfirmationDialog(
                title = "Move to Trash",
                message = "Are you sure you want to move '${currentGroup.displayName}' (including paired RAW/video files) to Trash?",
                onConfirm = {
                    showDeleteDialog = false
                    val targetIndex = safeCurrentPage
                    val groupToDelete = currentGroup
                    viewModel.moveGroupToTrash(groupToDelete) { success ->
                        if (success) {
                            val newList = displayedGroups.toMutableList().apply {
                                removeAt(targetIndex)
                            }
                            if (newList.isEmpty()) {
                                onNavigateBack()
                            } else {
                                displayedGroups = newList
                                val nextIndex = targetIndex.coerceIn(0, newList.size - 1)
                                coroutineScope.launch {
                                    pagerState.scrollToPage(nextIndex)
                                }
                            }
                        }
                    }
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

/**
 * ピンチズーム・ダブルタップ拡大・ドラッグパン操作に対応した画像ビューアコンポーザブル
 *
 * HorizontalPager の横スワイプ移動と干渉しないよう、拡大（scale > 1f）時のみパン操作のジェスチャーイベントを消費します。
 */
@Composable
fun ZoomablePhotoView(
    mediaGroup: MediaGroup,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var scale by remember(mediaGroup.baseName) { mutableFloatStateOf(1f) }
    var offset by remember(mediaGroup.baseName) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(mediaGroup.baseName) {
        scale = 1f
        offset = Offset.Zero
    }

    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(mediaGroup.baseName) {
                    detectTapGestures(
                        onTap = { onTap() },
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
                .pointerInput(mediaGroup.baseName) {
                    awaitEachGesture {
                        var zoom = 1f
                        var pan = Offset.Zero
                        var pastTouchSlop = false
                        val touchSlop = viewConfiguration.touchSlop

                        awaitFirstDown(requireUnconsumed = false)

                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                            if (!canceled) {
                                val pointerCount = event.changes.size
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()

                                // 2本指以上の操作(ピンチズーム) または 拡大状態(scale > 1f)でのパン操作のみポインターイベントを消費して処理する
                                if (pointerCount > 1 || scale > 1f || zoomChange != 1f) {
                                    if (!pastTouchSlop) {
                                        zoom *= zoomChange
                                        pan += panChange
                                        val centroidSize = event.calculateCentroidSize()
                                        val panMotion = pan.getDistance()
                                        val zoomMotion = abs(1 - zoom) * centroidSize

                                        if (panMotion > touchSlop || zoomMotion > touchSlop) {
                                            pastTouchSlop = true
                                        }
                                    }

                                    if (pastTouchSlop) {
                                        val newScale = (scale * zoomChange).coerceIn(1f, 10f)
                                        val maxOffsetX = (maxWidthPx * (newScale - 1f)) / 2f
                                        val maxOffsetY = (maxHeightPx * (newScale - 1f)) / 2f

                                        val newOffset = if (newScale > 1f) {
                                            Offset(
                                                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                        } else {
                                            Offset.Zero
                                        }

                                        scale = newScale
                                        offset = newOffset

                                        event.changes.forEach {
                                            if (it.positionChanged()) {
                                                it.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        } while (!canceled && event.changes.any { it.pressed })
                    }
                }
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
        }
    }
}

