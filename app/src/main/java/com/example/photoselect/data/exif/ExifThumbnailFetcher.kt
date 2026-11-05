package com.example.photoselect.data.exif

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Coil 画像読込ライブラリ用のカスタム [Fetcher] 実装
 *
 * 画像内の埋め込み EXIF サムネイルまたは動画ファイルのファーストフレームを高速抽出し、
 * ギャラリー一覧グリッドの描画パフォーマンスを大幅に向上させます。
 */
class ExifThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // 1. EXIF埋め込みサムネイルの抽出を試行
        val exifBitmap = ExifReader.readThumbnailBitmap(context, uri)
        if (exifBitmap != null) {
            return@withContext DrawableResult(
                drawable = exifBitmap.toDrawable(context.resources),
                isSampled = true,
                dataSource = DataSource.DISK,
            )
        }

        // 2. 動画ファイルの場合、MediaMetadataRetriever でフレーム抽出を試行
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val videoBitmap = retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            if (videoBitmap != null) {
                return@withContext DrawableResult(
                    drawable = videoBitmap.toDrawable(context.resources),
                    isSampled = true,
                    dataSource = DataSource.DISK,
                )
            }
        } catch (_: Exception) {
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return@withContext null
    }

    /**
     * Coil [Fetcher.Factory] 実装
     */
    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (isSupportedScheme(data.scheme)) {
                return ExifThumbnailFetcher(context, data)
            }
            return null
        }
    }

    companion object {
        /**
         * サポート対象のスキーム（"content", "file"）か確認します。
         */
        fun isSupportedScheme(scheme: String?): Boolean {
            return (scheme == "content") || (scheme == "file")
        }
    }
}
