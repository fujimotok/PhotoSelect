package com.example.photoselect.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.material.icons.automirrored.filled.Help
import com.github.fujimotok.photoselect.R
import com.example.photoselect.data.exif.ExifThumbnailFetcher
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.components.BadgeChip
import com.example.photoselect.ui.components.MoveToTrashConfirmationDialog
import kotlinx.coroutines.launch

/**
 * メインのギャラリー一覧グリッド画面コンポーザブル
 *
 * SAF（Storage Access Framework）によるフォルダ切り替え、グリッド一覧表示、
 * グループ化バッジ（RAW, VIDEO）、フィルタ・ソート機能、一括選択・削除・コピー・移動操作を提供します。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToDetail: (List<MediaGroup>, Int) -> Unit,
    gridState: LazyGridState = rememberLazyGridState(),
    viewModel: GalleryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen || uiState.isSelectionMode || uiState.showTrashScreen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (uiState.showTrashScreen) {
            viewModel.closeTrash()
        } else if (uiState.isSelectionMode) {
            viewModel.clearSelection()
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    val copyFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.copySelected(uri)
        }
    }

    val moveFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.moveSelected(uri)
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.infoMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
        uiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    if (uiState.showTrashScreen) {
        TrashScreen(
            uiState = uiState,
            onBack = { viewModel.closeTrash() },
            onRestore = { group -> viewModel.restoreTrashGroup(group) },
            onDeletePermanently = { group -> viewModel.permanentlyDeleteTrashGroup(group) },
            onEmptyTrash = { viewModel.emptyTrash() }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Photo Select",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Root Folders",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        if (uiState.rootFolders.isEmpty()) {
                            Text(
                                text = "No root folders",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            uiState.rootFolders.forEach { folder ->
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (folder.isCurrent) Icons.Default.FolderOpen else Icons.Default.Folder,
                                            contentDescription = null
                                        )
                                    },
                                    label = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = folder.name,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { viewModel.removeRootFolder(folder.uri) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Folder",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    selected = folder.isCurrent,
                                    onClick = {
                                        viewModel.selectRootFolder(folder.uri)
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch { drawerState.close() }
                                folderPickerLauncher.launch(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Add Root Folder")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = null) },
                            label = { Text("Settings") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToSettings()
                            }
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Help, contentDescription = null) },
                            label = { Text("Help & About") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onNavigateToHelp()
                            }
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            if (uiState.isSelectionMode) {
                                Text(
                                    text = "${uiState.selectedGroups.size} Selected",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                val currentFolder = uiState.rootFolders.firstOrNull { it.isCurrent }
                                Text(
                                    text = currentFolder?.name ?: "Photo Select",
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            if (uiState.isSelectionMode) {
                                IconButton(onClick = { viewModel.clearSelection() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Selection"
                                    )
                                }
                            } else {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Menu"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (uiState.isSelectionMode) {
                                // Copy Selected
                                IconButton(onClick = { copyFolderPickerLauncher.launch(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Selected"
                                    )
                                }
                                // Move Selected
                                IconButton(onClick = { moveFolderPickerLauncher.launch(null) }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                                        contentDescription = "Move Selected"
                                    )
                                }
                                // Delete Selected (to Trash)
                                IconButton(onClick = { viewModel.requestDeleteSelected() }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected"
                                    )
                                }
                            } else {
                                // Trash Icon Button
                                IconButton(onClick = { viewModel.openTrash() }) {
                                    Icon(
                                        imageVector = Icons.Default.RestoreFromTrash,
                                        contentDescription = "Trash"
                                    )
                                }
                                if (uiState.treeUri != null) {
                                    IconButton(onClick = { viewModel.refreshMediaGroups() }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Refresh Folder"
                                        )
                                    }
                                }
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (uiState.treeUri != null && !uiState.isLoading && uiState.rawMediaGroups.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = uiState.filterOption == GalleryFilter.ALL,
                                    onClick = { viewModel.setFilter(GalleryFilter.ALL) },
                                    label = { Text("All") }
                                )
                                FilterChip(
                                    selected = uiState.filterOption == GalleryFilter.PHOTOS_ONLY,
                                    onClick = { viewModel.setFilter(GalleryFilter.PHOTOS_ONLY) },
                                    label = { Text("Photos") }
                                )
                                FilterChip(
                                    selected = uiState.filterOption == GalleryFilter.VIDEOS_ONLY,
                                    onClick = { viewModel.setFilter(GalleryFilter.VIDEOS_ONLY) },
                                    label = { Text("Videos") }
                                )
                                FilterChip(
                                    selected = uiState.filterOption == GalleryFilter.RAW_AVAILABLE,
                                    onClick = { viewModel.setFilter(GalleryFilter.RAW_AVAILABLE) },
                                    label = { Text("RAW Only") }
                                )
                            }

                            var showSortMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Sort,
                                        contentDescription = "Sort"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Date: Newest First") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.DATE_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Date: Oldest First") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.DATE_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name: A to Z") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.NAME_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Name: Z to A") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.NAME_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size: Largest First") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.SIZE_DESC)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Size: Smallest First") },
                                        onClick = {
                                            viewModel.setSort(GallerySort.SIZE_ASC)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        if (uiState.loadingProgress in 0.01f..0.99f) {
                                            CircularProgressIndicator(
                                                progress = { uiState.loadingProgress },
                                                modifier = Modifier.size(64.dp)
                                            )
                                        } else {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                        Text(
                                            text = uiState.loadingMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            uiState.treeUri == null -> {
                                EmptyFolderState(
                                    message = "No folder selected.\nSelect a folder to view photos.",
                                    onSelectFolderClick = { folderPickerLauncher.launch(null) }
                                )
                            }

                            uiState.rawMediaGroups.isEmpty() -> {
                                EmptyFolderState(
                                    message = "No photo or video files found in the selected folder.",
                                    onSelectFolderClick = { folderPickerLauncher.launch(null) }
                                )
                            }

                            uiState.mediaGroups.isEmpty() -> {
                                EmptyFolderState(
                                    message = "No items match the selected filter.",
                                    onSelectFolderClick = {}
                                )
                            }

                            else -> {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Adaptive(minSize = 120.dp),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(
                                        items = uiState.mediaGroups,
                                        key = { group -> group.baseName + "_" + group.displayUri }
                                    ) { group ->
                                        val selectedIndex = uiState.selectedGroups.indexOfFirst { it.baseName == group.baseName }
                                        val isSelected = selectedIndex >= 0
                                        val displayIndex = if (isSelected) selectedIndex + 1 else null

                                        MediaGroupGridItem(
                                            group = group,
                                            isSelected = isSelected,
                                            selectionIndex = displayIndex,
                                            onCardClick = {
                                                if (uiState.isSelectionMode) {
                                                    viewModel.toggleGroupSelection(group)
                                                } else {
                                                    val index = uiState.mediaGroups.indexOfFirst { it.baseName == group.baseName }
                                                    if (index >= 0) {
                                                        onNavigateToDetail(uiState.mediaGroups, index)
                                                    }
                                                }
                                            },
                                            onCardLongClick = {
                                                viewModel.onItemLongClick(group)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.showDeleteConfirmDialog) {
                            DeleteSelectedConfirmationDialog(
                                selectedCount = uiState.selectedGroups.size,
                                onConfirm = { viewModel.confirmDeleteSelected() },
                                onDismiss = { viewModel.dismissDeleteDialog() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    uiState: GalleryUiState,
    onBack: () -> Unit,
    onRestore: (MediaGroup) -> Unit,
    onDeletePermanently: (MediaGroup) -> Unit,
    onEmptyTrash: () -> Unit
) {
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var groupToDeletePermanently by remember { mutableStateOf<MediaGroup?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash (${uiState.trashGroups.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Trash")
                    }
                },
                actions = {
                    if (uiState.trashGroups.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            if (uiState.loadingProgress in 0.01f..0.99f) {
                                CircularProgressIndicator(
                                    progress = { uiState.loadingProgress },
                                    modifier = Modifier.size(64.dp)
                                )
                            } else {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                            Text(
                                text = uiState.loadingMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                uiState.trashGroups.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Trash is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.trashGroups,
                            key = { group -> group.baseName + "_" + group.displayUri }
                        ) { group ->
                            TrashItemCard(
                                group = group,
                                onRestore = { onRestore(group) },
                                onDelete = { groupToDeletePermanently = group }
                            )
                        }
                    }
                }
            }
        }

        if (showEmptyTrashDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashDialog = false },
                title = { Text("Empty Trash?") },
                text = {
                    Text("Are you sure you want to permanently delete all ${uiState.trashGroups.size} item(s) in the trash? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showEmptyTrashDialog = false
                            onEmptyTrash()
                        }
                    ) {
                        Text(
                            text = "Empty Trash",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        groupToDeletePermanently?.let { group ->
            AlertDialog(
                onDismissRequest = { groupToDeletePermanently = null },
                title = { Text("Permanently Delete Item?") },
                text = {
                    Text("Are you sure you want to permanently delete '${group.displayName}'? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = groupToDeletePermanently
                            groupToDeletePermanently = null
                            target?.let { onDeletePermanently(it) }
                        }
                    ) {
                        Text(
                            text = "Delete Permanently",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { groupToDeletePermanently = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TrashItemCard(
    group: MediaGroup,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(group.displayUri)
                        .fetcherFactory(ExifThumbnailFetcher.Factory(context))
                        .crossfade(true)
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .build(),
                    contentDescription = group.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                if (group.hasRaw) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        BadgeChip(text = "RAW", containerColor = MaterialTheme.colorScheme.primaryContainer)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp)
            ) {
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onRestore,
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("Restore", fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGroupGridItem(
    group: MediaGroup,
    isSelected: Boolean,
    selectionIndex: Int?,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(group.displayUri)
                    .fetcherFactory(ExifThumbnailFetcher.Factory(context))
                    .crossfade(true)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.placeholder)
                    .build(),
                contentDescription = group.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video indicator overlay
            if (group.isVideo) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            }

            // RAW / Video Badges at Top Left
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (group.hasRaw) {
                    BadgeChip(text = "RAW", containerColor = MaterialTheme.colorScheme.primaryContainer)
                }
                if (group.isVideo) {
                    BadgeChip(text = "VIDEO", containerColor = MaterialTheme.colorScheme.secondaryContainer)
                }
            }

            // Selection Indicator Badge at Top Right (Only shown when selected)
            if (isSelected && selectionIndex != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$selectionIndex",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Display Name label at Bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}



@Composable
fun EmptyFolderState(
    message: String,
    onSelectFolderClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectFolderClick) {
            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Select Folder")
        }
    }
}

@Composable
fun DeleteSelectedConfirmationDialog(
    selectedCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MoveToTrashConfirmationDialog(
        title = "Move Selected to Trash",
        message = "Are you sure you want to move the selected $selectedCount file group(s) (including paired RAW/video files) to Trash?",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
