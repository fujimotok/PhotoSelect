package com.example.photoselect.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.util.MimeTypeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * Storage Access Framework (SAF) を用いたストレージ入出力データリポジトリ
 *
 * ディレクトリのスキャン、RAW/JPEG/動画のベース名ペアリング、ファイルのコピー・移動、
 * ゴミ箱（Trashディレクトリ）機能、永続的権限の保持などを管理します。
 */
class SafRepository {

    /**
     * 指定されたフォルダ URI に対する読み書きの永続権限を取得します。
     *
     * @param context [Context]
     * @param uri 永続化を要求する Tree URI
     */
    fun takePersistablePermission(context: Context, uri: Uri) {
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 指定された Tree URI 配下のフォルダ構造を非同期でスキャンし、[MediaGroup] リストを構築します。
     *
     * @param context [Context]
     * @param treeUri スキャン対象のルートフォルダ URI
     * @param primaryExtensions プライマリ画像拡張子セット（例: JPG, HEIC）
     * @param rawExtensions RAW画像拡張子セット（例: RW2, ARW）
     * @param onProgress ファイル読み込み件数の進捗コールバック
     * @param onBatchLoaded 段階的なバッチ読込完了コールバック（プログレッシブ表示用）
     * @return 最終更新日時でソートされた [MediaGroup] リスト
     */
    suspend fun loadMediaGroups(
        context: Context,
        treeUri: Uri,
        primaryExtensions: Set<String>,
        rawExtensions: Set<String>,
        onProgress: (Int) -> Unit = {},
        onBatchLoaded: (List<MediaGroup>) -> Unit = {}
    ): List<MediaGroup> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val mediaFiles = mutableListOf<MediaFile>()

        var lastEmittedCount = 0
        val emitBatch = {
            if (mediaFiles.size - lastEmittedCount >= 30 || mediaFiles.size < 30) {
                lastEmittedCount = mediaFiles.size
                val groups = buildMediaGroupsUnsorted(mediaFiles, primaryExtensions, rawExtensions)
                onBatchLoaded(groups)
            }
        }

        scanDocumentDirectory(rootDoc, mediaFiles, skipTrash = true, onProgress = onProgress, onBatchUpdate = emitBatch)

        val finalGroups = buildMediaGroupsSorted(mediaFiles, primaryExtensions, rawExtensions)
        onBatchLoaded(finalGroups)
        return@withContext finalGroups
    }

    /**
     * 読み込んだファイル群をベース名（拡張子を除いた名前）でグループ化します（ソートなし）。
     */
    private fun buildMediaGroupsUnsorted(
        mediaFiles: List<MediaFile>,
        primaryExtensions: Set<String>,
        rawExtensions: Set<String>
    ): List<MediaGroup> {
        // ベース名でグループ化（順序保持のため LinkedHashMap を使用）
        val groupsByBaseName = linkedMapOf<String, MutableList<MediaFile>>()
        for (file in mediaFiles) {
            val baseName = getBaseName(file.name)
            groupsByBaseName.getOrPut(baseName) { mutableListOf() }.add(file)
        }

        val result = mutableListOf<MediaGroup>()

        for ((baseName, files) in groupsByBaseName) {
            var primary: MediaFile? = null
            var raw: MediaFile? = null
            var video: MediaFile? = null
            val others = mutableListOf<MediaFile>()

            for (file in files) {
                val ext = file.extension.uppercase()
                if (primaryExtensions.contains(ext) && primary == null) {
                    primary = file
                } else if (rawExtensions.contains(ext) && raw == null) {
                    raw = file
                } else if (MediaGroup.isVideoExtension(ext) && video == null) {
                    video = file
                } else {
                    others.add(file)
                }
            }

            result.add(
                MediaGroup(
                    baseName = baseName,
                    primaryFile = primary,
                    rawFile = raw,
                    videoFile = video,
                    otherFiles = others
                )
            )
        }

        return result
    }

    /**
     * グループ化したメディア群を最終更新日時の降順でソートします。
     */
    private fun buildMediaGroupsSorted(
        mediaFiles: List<MediaFile>,
        primaryExtensions: Set<String>,
        rawExtensions: Set<String>
    ): List<MediaGroup> {
        return buildMediaGroupsUnsorted(mediaFiles, primaryExtensions, rawExtensions).sortedByDescending { it.lastModified }
    }

    /**
     * メディアグループの削除またはゴミ箱移動を実行します。
     *
     * @param treeUri treeUri が指定されている場合は Trash ディレクトリへ移動、指定がない場合は完全削除
     */
    suspend fun deleteMediaGroup(context: Context, group: MediaGroup, treeUri: Uri? = null): Boolean {
        return if (treeUri != null) {
            moveToTrash(context, group, treeUri)
        } else {
            permanentlyDeleteMediaGroup(context, group)
        }
    }

    /**
     * メディアグループに含まれる全ファイルをルートフォルダ配下の Trash ディレクトリへ移動します。
     */
    suspend fun moveToTrash(context: Context, group: MediaGroup, treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
        val trashDir = rootDoc.findFile("Trash") ?: rootDoc.createDirectory("Trash") ?: return@withContext false
        var success = true

        for (file in group.allFiles) {
            if (copyMediaFile(context, file, trashDir)) {
                // ゴミ箱へのコピー成功後に元ファイルを削除
                deleteMediaFile(context, file)
            } else {
                success = false
            }
        }
        return@withContext success
    }

    /**
     * ゴミ箱（Trash ディレクトリ）内のメディアグループ一覧を読み込みます。
     */
    suspend fun loadTrashMediaGroups(
        context: Context,
        treeUri: Uri,
        primaryExtensions: Set<String>,
        rawExtensions: Set<String>
    ): List<MediaGroup> = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val trashDir = rootDoc.findFile("Trash") ?: return@withContext emptyList()
        val mediaFiles = mutableListOf<MediaFile>()

        scanDocumentDirectory(trashDir, mediaFiles)

        return@withContext buildMediaGroupsSorted(mediaFiles, primaryExtensions, rawExtensions)
    }

    /**
     * ゴミ箱内のメディアグループを元のルートフォルダへ復元します。
     */
    suspend fun restoreMediaGroup(context: Context, group: MediaGroup, treeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext false
        var success = true

        for (file in group.allFiles) {
            if (copyMediaFile(context, file, rootDoc)) {
                deleteMediaFile(context, file)
            } else {
                success = false
            }
        }
        return@withContext success
    }

    /**
     * メディアグループ内のすべてのファイルを永続的にストレージから削除します。
     */
    suspend fun permanentlyDeleteMediaGroup(context: Context, group: MediaGroup): Boolean = withContext(Dispatchers.IO) {
        var success = true
        for (file in group.allFiles) {
            if (!deleteMediaFile(context, file)) {
                success = false
            }
        }
        return@withContext success
    }

    /**
     * メディアグループ内のすべてのファイルを指定の宛先フォルダへコピーします。
     */
    suspend fun copyMediaGroup(context: Context, group: MediaGroup, destTreeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val destDir = DocumentFile.fromTreeUri(context, destTreeUri) ?: return@withContext false
        var success = true

        for (file in group.allFiles) {
            if (!copyMediaFile(context, file, destDir)) {
                success = false
            }
        }
        return@withContext success
    }

    /**
     * メディアグループ内のすべてのファイルを指定の宛先フォルダへ移動（コピー後、元ファイルを削除）します。
     */
    suspend fun moveMediaGroup(context: Context, group: MediaGroup, destTreeUri: Uri): Boolean = withContext(Dispatchers.IO) {
        val copied = copyMediaGroup(context, group, destTreeUri)
        if (copied) {
            permanentlyDeleteMediaGroup(context, group)
        } else {
            false
        }
    }

    /**
     * 単一メディアファイルを宛先 [DocumentFile] ディレクトリへコピーします。
     */
    private fun copyMediaFile(context: Context, mediaFile: MediaFile, destDir: DocumentFile): Boolean {
        val srcUri = mediaFile.uri ?: return false
        val mimeType = MimeTypeUtils.getMimeType(mediaFile.extension)
        val destFile = destDir.createFile(mimeType, mediaFile.name) ?: return false

        return try {
            context.contentResolver.openInputStream(srcUri)?.use { input ->
                context.contentResolver.openOutputStream(destFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 単一メディアファイルをストレージから削除します。
     */
    private fun deleteMediaFile(context: Context, mediaFile: MediaFile): Boolean {
        return try {
            val uri = mediaFile.uri
            val docFile = mediaFile.documentFile ?: if (uri != null) DocumentFile.fromSingleUri(context, uri) else null
            if (docFile != null && docFile.exists()) {
                docFile.delete()
            } else if (uri != null) {
                context.contentResolver.delete(uri, null, null) > 0
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * DocumentFile ディレクトリを再帰的にスキャンしてファイルを抽出します。
     */
    private suspend fun scanDocumentDirectory(
        docDir: DocumentFile,
        result: MutableList<MediaFile>,
        skipTrash: Boolean = false,
        onProgress: (Int) -> Unit = {},
        onBatchUpdate: () -> Unit = {}
    ) {
        val files = docDir.listFiles()
        for (file in files) {
            yield()
            if (file.isDirectory) {
                if (skipTrash && file.name.equals("Trash", ignoreCase = true)) {
                    continue
                }
                scanDocumentDirectory(file, result, skipTrash, onProgress, onBatchUpdate)
            } else if (file.isFile) {
                val name = file.name ?: continue
                val ext = getExtension(name)
                val mediaFile = MediaFile(
                    uri = file.uri,
                    name = name,
                    extension = ext,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    documentFile = file
                )
                result.add(mediaFile)
                onProgress(result.size)
                if (result.size % 30 == 0) {
                    onBatchUpdate()
                }
            }
        }
    }

    /** ファイル名から拡張子を除いたベース名を取得 */
    private fun getBaseName(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
    }

    /** ファイル名から拡張子を抽出して大文字変換 */
    private fun getExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1).uppercase()
        } else {
            ""
        }
    }
}
