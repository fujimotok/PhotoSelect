package com.example.photoselect.data.model

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * 個別のメディアファイル情報を保持するデータクラス
 *
 * SAF（Storage Access Framework）等から取得した単一ファイルのメタデータ情報を管理します。
 *
 * @property uri ファイルの Content URI
 * @property name 拡張子を含むファイル名（例: "DSC0001.JPG"）
 * @property extension 大文字に統一された拡張子（例: "JPG", "RW2"）
 * @property size ファイルサイズ（バイト単位）
 * @property lastModified 最終更新日時（エポックミリ秒）
 * @property documentFile SAF操作用の [DocumentFile] オブジェクト
 */
data class MediaFile(
    val uri: Uri? = null,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val documentFile: DocumentFile? = null
)
