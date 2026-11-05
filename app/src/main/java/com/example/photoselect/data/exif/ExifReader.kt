package com.example.photoselect.data.exif

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.photoselect.data.model.MediaGroup
import java.io.InputStream

/**
 * [ExifInterface] を使用して画像や各種メディアファイルの EXIF メタデータを解析するユーティリティオブジェクト
 */
object ExifReader {

    /**
     * [MediaGroup] から最適なファイル（Primary, RAW, Videoの順）を選択し、EXIF情報を読み込みます。
     *
     * @param context [Context]
     * @param group 読み込み対象の [MediaGroup]
     * @return 解析された [ExifMetadata]
     */
    fun readExif(context: Context, group: MediaGroup): ExifMetadata {
        val targetFile = group.primaryFile ?: group.rawFile ?: group.videoFile ?: return ExifMetadata()
        val uri = targetFile.uri ?: return ExifMetadata()
        return readExifFromUri(context, uri)
    }

    /**
     * 指定された [Uri] から InputStream を開いて EXIF メタデータを解析します。
     *
     * @param context [Context]
     * @param uri 対象の [Uri]
     * @return 解析された [ExifMetadata]
     */
    fun readExifFromUri(context: Context, uri: Uri): ExifMetadata {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                parseExifFromStream(inputStream)
            } ?: ExifMetadata()
        } catch (e: Exception) {
            e.printStackTrace()
            ExifMetadata()
        }
    }

    /**
     * InputStream から [ExifInterface] 経由で EXIF タグ（カメラメーカー、モデル、シャッタースピード、絞り等）を取得・整形します。
     */
    private fun parseExifFromStream(inputStream: InputStream): ExifMetadata {
        val exif = ExifInterface(inputStream)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()
        val lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()

        val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
            ?: @Suppress("DEPRECATION") exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)

        // シャッタースピードの分数・秒数フォーマット計算
        val exposureTimeDouble = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
        val shutterSpeed = if (exposureTimeDouble > 0) {
            if (exposureTimeDouble < 1.0) {
                "1/${(1.0 / exposureTimeDouble).toInt()}s"
            } else {
                "${exposureTimeDouble}s"
            }
        } else {
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        }

        // 絞り値 (f-number)
        val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
        val aperture = if (fNumber > 0) "f/$fNumber" else exif.getAttribute(ExifInterface.TAG_F_NUMBER)

        // 焦点距離
        val focalLengthVal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
        val focalLength = if (focalLengthVal > 0) "${focalLengthVal.toInt()}mm" else null

        // 解像度（幅・高さ）
        var width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
        var height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        if (width == 0 || height == 0) {
            width = exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
            height = exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)
        }

        // 撮影日時
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

        return ExifMetadata(
            make = make,
            model = model,
            lensModel = lensModel,
            iso = iso,
            shutterSpeed = shutterSpeed,
            aperture = aperture,
            focalLength = focalLength,
            width = width,
            height = height,
            dateTime = dateTime,
        )
    }

    /**
     * 画像ファイル内に埋め込まれた EXIF サムネイルビットマップを取得します。
     *
     * @param context [Context]
     * @param uri 画像の [Uri]
     * @return 埋め込みサムネイルが存在する場合は [Bitmap]、なければ null
     */
    fun readThumbnailBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                if (exif.hasThumbnail()) {
                    exif.thumbnailBitmap
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
