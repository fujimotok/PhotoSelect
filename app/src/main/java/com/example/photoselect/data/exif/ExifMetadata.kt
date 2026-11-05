package com.example.photoselect.data.exif

/**
 * 撮影情報（EXIFメタデータ）を保持するデータクラス
 *
 * カメラ機種、レンズ、シャッタースピード、絞り値、ISO、解像度、撮影日時などを格納し、
 * 表示用テキストの整形プロパティを提供します。
 *
 * @property make カメラメーカー（例: "Panasonic", "SONY"）
 * @property model カメラモデル名（例: "DC-S5M2", "ILCE-7M4"）
 * @property lensModel レンズモデル名
 * @property iso ISO感度
 * @property shutterSpeed シャッタースピード表示用文字列（例: "1/250s", "2s"）
 * @property aperture 絞り値表示用文字列（例: "f/2.8"）
 * @property focalLength 焦点距離（例: "50mm"）
 * @property width 画像の横幅（ピクセル）
 * @property height 画像の高さ（ピクセル）
 * @property dateTime 撮影日時文字列
 */
data class ExifMetadata(
    val make: String? = null,
    val model: String? = null,
    val lensModel: String? = null,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val dateTime: String? = null
) {
    /**
     * 表示用のカメラ・メーカー整形名を取得します。
     * 重複するメーカー名を排除して読みやすく表示します。
     */
    val cameraDisplay: String
        get() = when {
            !make.isNullOrEmpty() && !model.isNullOrEmpty() -> {
                if (model.startsWith(make, ignoreCase = true)) model else "$make $model"
            }
            !model.isNullOrEmpty() -> model
            !make.isNullOrEmpty() -> make
            else -> "Unknown Camera"
        }

    /**
     * シャッタースピード、絞り、ISO、焦点距離を結合した露出パラメータ表示文字列を取得します。
     * 例: "1/250s  |  f/2.8  |  ISO 100  |  50mm"
     */
    val exposureDisplay: String
        get() {
            val parts = mutableListOf<String>()
            if (!shutterSpeed.isNullOrEmpty()) parts.add(shutterSpeed)
            if (!aperture.isNullOrEmpty()) parts.add(aperture)
            if (!iso.isNullOrEmpty()) parts.add("ISO $iso")
            if (!focalLength.isNullOrEmpty()) parts.add(focalLength)
            return if (parts.isNotEmpty()) parts.joinToString("  |  ") else "N/A"
        }

    /**
     * 解像度の表示用文字列を取得します。（例: "6000 x 4000"）
     */
    val resolutionDisplay: String
        get() = if (width > 0 && height > 0) "${width} x ${height}" else "Unknown resolution"
}
