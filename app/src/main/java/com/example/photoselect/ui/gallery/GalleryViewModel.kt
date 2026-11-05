package com.example.photoselect.ui.gallery

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.data.saf.SafRepository
import com.example.photoselect.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ルートフォルダ情報（名前・URI・アクティブフラグ）
 */
data class RootFolderInfo(
    val uri: Uri,
    val name: String,
    val isCurrent: Boolean
)

/** ギャラリーフィルタ条件 */
enum class GalleryFilter {
    /** すべてを表示 */
    ALL,
    /** 写真（静止画）のみ表示 */
    PHOTOS_ONLY,
    /** 動画のみ表示 */
    VIDEOS_ONLY,
    /** RAWペアが存在する項目のみ表示 */
    RAW_AVAILABLE
}

/** ギャラリーソート条件 */
enum class GallerySort {
    /** 日時（新しい順） */
    DATE_DESC,
    /** 日時（古い順） */
    DATE_ASC,
    /** ファイル名（昇順） */
    NAME_ASC,
    /** ファイル名（降順） */
    NAME_DESC,
    /** ファイルサイズ（大きい順） */
    SIZE_DESC,
    /** ファイルサイズ（小さい順） */
    SIZE_ASC
}

/**
 * ギャラリー画面全体のUI状態を保持するデータクラス
 */
data class GalleryUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Float = 0f,
    val loadingMessage: String = "Loading gallery...",
    val treeUri: Uri? = null,
    val rootFolders: List<RootFolderInfo> = emptyList(),
    val rawMediaGroups: List<MediaGroup> = emptyList(),
    val mediaGroups: List<MediaGroup> = emptyList(),
    val filterOption: GalleryFilter = GalleryFilter.ALL,
    val sortOption: GallerySort = GallerySort.DATE_DESC,
    val selectedGroups: List<MediaGroup> = emptyList(),
    val showDeleteConfirmDialog: Boolean = false,
    val showTrashScreen: Boolean = false,
    val trashGroups: List<MediaGroup> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    /** 1つ以上の項目が選択されているかどうか */
    val isSelectionMode: Boolean
        get() = selectedGroups.isNotEmpty()
}

/**
 * ギャラリー一覧画面の状態管理、ファイル操作（スキャン、移動、コピー、ゴミ箱管理等）を担う ViewModel
 */
class GalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val safRepository = SafRepository()

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.treeUriFlow,
                settingsRepository.primaryExtensionsFlow,
                settingsRepository.rawExtensionsFlow
            ) { treeUriStr, primaryExts, rawExts ->
                Triple(treeUriStr, primaryExts, rawExts)
            }.collect { (treeUriStr, primaryExts, rawExts) ->
                val treeUri = treeUriStr?.let { Uri.parse(it) }
                _uiState.update { it.copy(treeUri = treeUri) }
                loadRootFolders()
                if (treeUri != null) {
                    loadMediaGroups(treeUri, primaryExts, rawExts)
                } else {
                    _uiState.update {
                        it.copy(
                            rawMediaGroups = emptyList(),
                            mediaGroups = emptyList(),
                            selectedGroups = emptyList(),
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    /** 永続的権限が保持されているルートフォルダ一覧を読み込みます */
    fun loadRootFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val persistedPermissions = context.contentResolver.persistedUriPermissions
            val currentUri = uiState.value.treeUri

            val folders = persistedPermissions.map { permission ->
                val uri = permission.uri
                val docFile = DocumentFile.fromTreeUri(context, uri)
                val name = docFile?.name ?: uri.lastPathSegment ?: "Folder"
                RootFolderInfo(
                    uri = uri,
                    name = name,
                    isCurrent = (uri == currentUri)
                )
            }
            _uiState.update { it.copy(rootFolders = folders) }
        }
    }

    /** カレントのルートフォルダを切り替えます */
    fun selectRootFolder(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.saveTreeUri(uri.toString())
            loadRootFolders()
        }
    }

    /** 保持されているルートフォルダ権限を解放・除去します */
    fun removeRootFolder(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val currentUri = uiState.value.treeUri
            if (currentUri == uri) {
                val remaining = context.contentResolver.persistedUriPermissions
                val nextUri = remaining.firstOrNull()?.uri
                settingsRepository.saveTreeUri(nextUri?.toString())
            }
            loadRootFolders()
        }
    }

    /** 新しいフォルダが SAF 経由で選択された際に永続権限を取得して保存します */
    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            safRepository.takePersistablePermission(getApplication(), uri)
            settingsRepository.saveTreeUri(uri.toString())
            loadRootFolders()
        }
    }

    /** 現在選択中のルートフォルダ内のメディアグループを再読み込みします */
    fun refreshMediaGroups() {
        val treeUri = uiState.value.treeUri ?: return
        viewModelScope.launch {
            val primaryExts = settingsRepository.primaryExtensionsFlow.first()
            val rawExts = settingsRepository.rawExtensionsFlow.first()
            loadMediaGroups(treeUri, primaryExts, rawExts)
        }
    }

    /** 指定フォルダを走査してメディアグループを作成し、進捗およびプログレッシブ更新を行います */
    private fun loadMediaGroups(treeUri: Uri, primaryExts: Set<String>, rawExts: Set<String>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Scanning files...",
                    errorMessage = null
                )
            }
            try {
                var isFirstBatch = true
                val groups = safRepository.loadMediaGroups(
                    getApplication(),
                    treeUri,
                    primaryExts,
                    rawExts,
                    onProgress = { count ->
                        val progress = 1f - (1f / (1f + count * 0.05f))
                        _uiState.update { state ->
                            state.copy(
                                loadingProgress = progress.coerceIn(0f, 0.95f),
                                loadingMessage = "Scanning files... ($count found)"
                            )
                        }
                    },
                    onBatchLoaded = { batchGroups ->
                        _uiState.update { state ->
                            val validSelected = state.selectedGroups.filter { sel ->
                                batchGroups.any { it.baseName == sel.baseName }
                            }
                            val shouldStopLoading = isFirstBatch && batchGroups.isNotEmpty()
                            if (shouldStopLoading) {
                                isFirstBatch = false
                            }
                            val processedGroups = applyFilterAndSort(batchGroups, state.filterOption, state.sortOption)
                            state.copy(
                                rawMediaGroups = batchGroups,
                                mediaGroups = processedGroups,
                                selectedGroups = validSelected,
                                isLoading = if (shouldStopLoading) false else state.isLoading,
                                loadingProgress = if (shouldStopLoading) 1f else state.loadingProgress,
                                loadingMessage = if (shouldStopLoading) "Complete" else state.loadingMessage
                            )
                        }
                    }
                )
                _uiState.update { state ->
                    val validSelected = state.selectedGroups.filter { sel ->
                        groups.any { it.baseName == sel.baseName }
                    }
                    val processedGroups = applyFilterAndSort(groups, state.filterOption, state.sortOption)
                    state.copy(
                        rawMediaGroups = groups,
                        mediaGroups = processedGroups,
                        selectedGroups = validSelected,
                        isLoading = false,
                        loadingProgress = 1f,
                        loadingMessage = "Complete"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingProgress = 1f,
                        errorMessage = "Failed to load folder: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /** フィルタ条件を設定します */
    fun setFilter(filter: GalleryFilter) {
        _uiState.update { state ->
            val processed = applyFilterAndSort(state.rawMediaGroups, filter, state.sortOption)
            state.copy(filterOption = filter, mediaGroups = processed)
        }
    }

    /** ソート条件を設定します */
    fun setSort(sort: GallerySort) {
        _uiState.update { state ->
            val processed = applyFilterAndSort(state.rawMediaGroups, state.filterOption, sort)
            state.copy(sortOption = sort, mediaGroups = processed)
        }
    }

    /** フィルタおよびソート処理を適用します */
    private fun applyFilterAndSort(
        groups: List<MediaGroup>,
        filter: GalleryFilter,
        sort: GallerySort
    ): List<MediaGroup> {
        val filtered = when (filter) {
            GalleryFilter.ALL -> groups
            GalleryFilter.PHOTOS_ONLY -> groups.filter { !it.isVideo }
            GalleryFilter.VIDEOS_ONLY -> groups.filter { it.isVideo }
            GalleryFilter.RAW_AVAILABLE -> groups.filter { it.hasRaw }
        }

        return when (sort) {
            GallerySort.DATE_DESC -> filtered.sortedByDescending { it.lastModified }
            GallerySort.DATE_ASC -> filtered.sortedBy { it.lastModified }
            GallerySort.NAME_ASC -> filtered.sortedBy { it.baseName.lowercase() }
            GallerySort.NAME_DESC -> filtered.sortedByDescending { it.baseName.lowercase() }
            GallerySort.SIZE_DESC -> filtered.sortedByDescending { group -> group.allFiles.sumOf { it.size } }
            GallerySort.SIZE_ASC -> filtered.sortedBy { group -> group.allFiles.sumOf { it.size } }
        }
    }

    /** 長押しで選択モードを開始または選択を追加します */
    fun onItemLongClick(group: MediaGroup) {
        _uiState.update { state ->
            val currentSelected = state.selectedGroups.toMutableList()
            if (currentSelected.none { it.baseName == group.baseName }) {
                currentSelected.add(group)
            }
            state.copy(selectedGroups = currentSelected)
        }
    }

    /** 個別項目の選択状態をトグルします */
    fun toggleGroupSelection(group: MediaGroup) {
        _uiState.update { state ->
            val currentSelected = state.selectedGroups.toMutableList()
            val existingIndex = currentSelected.indexOfFirst { it.baseName == group.baseName }
            if (existingIndex >= 0) {
                currentSelected.removeAt(existingIndex)
            } else {
                currentSelected.add(group)
            }
            state.copy(selectedGroups = currentSelected, errorMessage = null)
        }
    }

    /** すべての選択を解除します */
    fun clearSelection() {
        _uiState.update { it.copy(selectedGroups = emptyList()) }
    }

    /** エラー・情報メッセージをクリアします */
    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    /** 選択項目のゴミ箱移動確認ダイアログを表示します */
    fun requestDeleteSelected() {
        if (uiState.value.selectedGroups.isNotEmpty()) {
            _uiState.update { it.copy(showDeleteConfirmDialog = true) }
        }
    }

    /** 削除確認ダイアログを閉じます */
    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    /** 単一のメディアグループをゴミ箱（Trash ディレクトリ）へ移動します */
    fun moveGroupToTrash(group: MediaGroup, onComplete: (Boolean) -> Unit = {}) {
        val treeUri = uiState.value.treeUri
        if (treeUri == null) {
            _uiState.update { it.copy(errorMessage = "No root folder selected") }
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Moving '${group.displayName}' to Trash..."
                )
            }
            val success = safRepository.moveToTrash(getApplication(), group, treeUri)
            if (success) {
                _uiState.update { state ->
                    state.copy(infoMessage = "Moved '${group.displayName}' to Trash")
                }
                refreshMediaGroups()
            } else {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "Failed to move '${group.displayName}' to Trash"
                    )
                }
            }
            onComplete(success)
        }
    }

    /** 選択された複数のメディアグループをゴミ箱へ一括移動します */
    fun moveGroupsToTrash(groups: List<MediaGroup>, onComplete: (Int) -> Unit = {}) {
        val treeUri = uiState.value.treeUri
        if (groups.isEmpty() || treeUri == null) {
            onComplete(0)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showDeleteConfirmDialog = false,
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Moving to Trash... (0/${groups.size})"
                )
            }
            var trashedCount = 0
            groups.forEachIndexed { index, group ->
                if (safRepository.moveToTrash(getApplication(), group, treeUri)) {
                    trashedCount++
                }
                val count = index + 1
                val progress = count.toFloat() / groups.size
                _uiState.update { state ->
                    state.copy(
                        loadingProgress = progress,
                        loadingMessage = "Moving to Trash... ($count/${groups.size})"
                    )
                }
            }
            _uiState.update { state ->
                state.copy(
                    selectedGroups = emptyList(),
                    infoMessage = if (trashedCount > 0) "Moved $trashedCount item(s) to Trash" else null,
                    errorMessage = if (trashedCount < groups.size) "Some items could not be moved to Trash" else null
                )
            }
            refreshMediaGroups()
            onComplete(trashedCount)
        }
    }

    /** 削除ダイアログでの確定時に選択項目をゴミ箱へ一括移動します */
    fun confirmDeleteSelected() {
        moveGroupsToTrash(uiState.value.selectedGroups)
    }

    /** ゴミ箱画面を開き、ゴミ箱内のファイルを読み込みます */
    fun openTrash() {
        _uiState.update { it.copy(showTrashScreen = true) }
        loadTrashGroups()
    }

    /** ゴミ箱画面を閉じます */
    fun closeTrash() {
        _uiState.update { it.copy(showTrashScreen = false) }
    }

    /** ゴミ箱内のメディアグループ一覧を読み込みます */
    fun loadTrashGroups() {
        val treeUri = uiState.value.treeUri ?: return
        viewModelScope.launch {
            val primaryExts = settingsRepository.primaryExtensionsFlow.first()
            val rawExts = settingsRepository.rawExtensionsFlow.first()
            try {
                val trash = safRepository.loadTrashMediaGroups(getApplication(), treeUri, primaryExts, rawExts)
                _uiState.update { it.copy(trashGroups = trash) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** ゴミ箱内のメディアグループを元の場所に復元します */
    fun restoreTrashGroup(group: MediaGroup) {
        val treeUri = uiState.value.treeUri ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Restoring '${group.displayName}'..."
                )
            }
            val success = safRepository.restoreMediaGroup(getApplication(), group, treeUri)
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (success) "Restored item successfully" else null,
                    errorMessage = if (!success) "Failed to restore item" else null
                )
            }
            loadTrashGroups()
            refreshMediaGroups()
        }
    }

    /** ゴミ箱内のメディアグループを完全にストレージから消去します */
    fun permanentlyDeleteTrashGroup(group: MediaGroup) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Deleting '${group.displayName}'..."
                )
            }
            val success = safRepository.permanentlyDeleteMediaGroup(getApplication(), group)
            _uiState.update { state ->
                state.copy(
                    infoMessage = if (success) "Permanently deleted item" else null,
                    errorMessage = if (!success) "Failed to delete item" else null
                )
            }
            loadTrashGroups()
        }
    }

    /** ゴミ箱内のすべてのファイルを完全消去します */
    fun emptyTrash() {
        val trashGroups = uiState.value.trashGroups
        if (trashGroups.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "Emptying trash... (0/${trashGroups.size})"
                )
            }
            var successCount = 0
            trashGroups.forEachIndexed { index, group ->
                if (safRepository.permanentlyDeleteMediaGroup(getApplication(), group)) {
                    successCount++
                }
                val count = index + 1
                val progress = count.toFloat() / trashGroups.size
                _uiState.update { state ->
                    state.copy(
                        loadingProgress = progress,
                        loadingMessage = "Emptying trash... ($count/${trashGroups.size})"
                    )
                }
            }
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    loadingProgress = 1f,
                    loadingMessage = "Complete",
                    infoMessage = "Emptied trash ($successCount items deleted)",
                    trashGroups = emptyList()
                )
            }
        }
    }

    /** 選択されたメディアグループを指定宛先フォルダへコピーします */
    fun copySelected(destUri: Uri) {
        executeBatchActionOnSelected("Copied", "copy") { group ->
            safRepository.copyMediaGroup(getApplication(), group, destUri)
        }
    }

    /** 選択されたメディアグループを指定宛先フォルダへ移動します */
    fun moveSelected(destUri: Uri) {
        executeBatchActionOnSelected("Moved", "move") { group ->
            safRepository.moveMediaGroup(getApplication(), group, destUri)
        }
    }

    /** バッチアクション（コピー/移動）を共通実行します */
    private fun executeBatchActionOnSelected(
        pastActionName: String,
        actionVerb: String,
        action: suspend (MediaGroup) -> Boolean
    ) {
        val targets = uiState.value.selectedGroups
        if (targets.isEmpty()) return

        val capitalizedVerb = actionVerb.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    loadingProgress = 0f,
                    loadingMessage = "${capitalizedVerb}ing items... (0/${targets.size})"
                )
            }
            var successCount = 0
            targets.forEachIndexed { index, group ->
                if (action(group)) {
                    successCount++
                }
                val count = index + 1
                val progress = count.toFloat() / targets.size
                _uiState.update { state ->
                    state.copy(
                        loadingProgress = progress,
                        loadingMessage = "${capitalizedVerb}ing items... ($count/${targets.size})"
                    )
                }
            }
            _uiState.update { state ->
                state.copy(
                    selectedGroups = emptyList(),
                    infoMessage = if (successCount > 0) "$pastActionName $successCount item(s)" else null,
                    errorMessage = if (successCount < targets.size) "Failed to $actionVerb some items" else null
                )
            }
            refreshMediaGroups()
        }
    }
}

