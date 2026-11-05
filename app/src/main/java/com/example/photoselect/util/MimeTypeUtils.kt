package com.example.photoselect.util

/**
 * 拡張子から適切な MIME タイプを取得するユーティリティオブジェクト
 */
object MimeTypeUtils {
    /**
     * ファイル拡張子に対応する MIME タイプ文字列を返します。
     *
     * @param extension 拡張子文字列（例: "JPG", "RW2", "MP4"）
     * @return MIME タイプ文字列（例: "image/jpeg", "video/mp4"）
     */
    fun getMimeType(extension: String): String {
        return when (extension.uppercase()) {
            "JPG", "JPEG" -> "image/jpeg"
            "PNG" -> "image/png"
            "WEBP" -> "image/webp"
            "GIF" -> "image/gif"
            "HIF", "HEIC" -> "image/heif"
            "MP4" -> "video/mp4"
            "MOV" -> "video/quicktime"
            "MKV" -> "video/x-matroska"
            "AVI" -> "video/x-msvideo"
            "3GP" -> "video/3gpp"
            "M4V" -> "video/x-m4v"
            "WEBM" -> "video/webm"
            "RW2" -> "image/x-panasonic-raw"
            "ARW" -> "image/x-sony-arw"
            "CR2", "CR3" -> "image/x-canon-cr2"
            "NEF" -> "image/x-nikon-nef"
            "DNG" -> "image/x-adobe-dng"
            "RAF" -> "image/x-fuji-raf"
            "ORF" -> "image/x-olympus-orf"
            else -> "application/octet-stream"
        }
    }
}
