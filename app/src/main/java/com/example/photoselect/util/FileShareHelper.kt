package com.example.photoselect.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.photoselect.data.model.MediaFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * [FileProvider] を使用して外部アプリへのファイル共有インテントを発行するヘルパーオブジェクト
 */
object FileShareHelper {

    /**
     * 指定された [MediaFile] を外部アプリに共有するための Intent を生成・発行します。
     *
     * Android 10+ のセキュリティ要件に準拠し、cacheDir/shared_media 内に一時コピーを作成して
     * FileProvider 経由で content:// URI を安全に割り当てます。
     *
     * @param context [Context]
     * @param mediaFile 共有対象の [MediaFile]
     */
    suspend fun shareMediaFile(context: Context, mediaFile: MediaFile) = withContext(Dispatchers.IO) {
        val uri = mediaFile.uri ?: return@withContext
        val shareableUri = prepareShareableUri(context, uri, mediaFile.name) ?: uri

        val mimeType = MimeTypeUtils.getMimeType(mediaFile.extension)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, shareableUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share ${mediaFile.name}")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * キャッシュディレクトリ内に安全な共有用ファイルをコピー作成し、FileProvider URIを返します。
     */
    private fun prepareShareableUri(context: Context, sourceUri: Uri, fileName: String): Uri? {
        return try {
            val shareDir = File(context.cacheDir, "shared_media")
            if (!shareDir.exists()) shareDir.mkdirs()

            val targetFile = File(shareDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
