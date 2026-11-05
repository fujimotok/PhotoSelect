package com.example.photoselect.data.model

import android.net.Uri

/**
 * ベースファイル名（拡張子を除いた同一名称）で関連付けられたメディアファイルのグループ
 *
 * 例: "DSC0001" というベース名に対し、Primary (JPEG/HEIF), RAW (.RW2, .ARW等), 動画 (.MP4等)
 * などのペアリングファイルを単一のユニットとしてカプセル化します。
 *
 * @property baseName 拡張子を除いた共通のファイル名（例: "DSC0001"）
 * @property primaryFile プライマリ画像ファイル（JPEG, HEIC 等）
 * @property rawFile ペアリングされたRAW画像ファイル（RW2, ARW, CR3 等）
 * @property videoFile 動画ファイル（MP4, MOV 等）
 * @property otherFiles その他の関連ファイル
 */
data class MediaGroup(
    val baseName: String,
    val primaryFile: MediaFile? = null,
    val rawFile: MediaFile? = null,
    val videoFile: MediaFile? = null,
    val otherFiles: List<MediaFile> = emptyList()
) {
    /** RAWファイルがペアとして存在するかどうか */
    val hasRaw: Boolean
        get() = rawFile != null

    /** 動画ファイルとして扱うかどうか */
    val isVideo: Boolean
        get() = videoFile != null || (primaryFile != null && isVideoExtension(primaryFile.extension))

    /** 一覧・プレビュー表示で使用する代表 URI */
    val displayUri: Uri?
        get() = primaryFile?.uri ?: videoFile?.uri ?: rawFile?.uri

    /** 一覧・詳細画面で表示する代表ファイル名 */
    val displayName: String
        get() = primaryFile?.name ?: videoFile?.name ?: rawFile?.name ?: baseName

    /** グループ内で最も新しい最終更新日時 */
    val lastModified: Long
        get() = listOfNotNull(primaryFile, rawFile, videoFile).plus(otherFiles)
            .maxOfOrNull { it.lastModified } ?: 0L

    /** グループに含まれるすべてのファイルリスト */
    val allFiles: List<MediaFile>
        get() = listOfNotNull(primaryFile, rawFile, videoFile).plus(otherFiles)

    companion object {
        /** 動画として認識する拡張子のセット */
        val VIDEO_EXTENSIONS = setOf("MP4", "MOV", "MKV", "AVI", "3GP", "M4V", "WEBM")

        /**
         * 指定された拡張子が動画形式かどうかを判定します。
         *
         * @param ext 判定対象の拡張子（大文字小文字不問）
         * @return 動画拡張子の場合 true
         */
        fun isVideoExtension(ext: String): Boolean {
            return VIDEO_EXTENSIONS.contains(ext.uppercase())
        }
    }
}
