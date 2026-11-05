# Photo Select - 開発者向けドキュメント (Developer Documentation)

本書は **Photo Select** アプリケーションのアーキテクチャ設計、コンポーネント構成、処理パイプライン、ビルドおよびテスト手順を解説する開発者向けドキュメントです。

---

## 🏗️ アーキテクチャ概要 (Architecture Overview)

本プロジェクトは Android の標準的な **MVVM (Model-View-ViewModel)** アーキテクチャおよび **Repository パターン** に準拠し、100% Jetpack Compose でUIを構築しています。

```
[ UI Layer (Jetpack Compose) ]
   └── MainScreen
         ├── GalleryScreen  <--> GalleryViewModel
         ├── DetailScreen   <--> GalleryViewModel / ExifReader / FileShareHelper
         ├── ComparisonScreen
         ├── SettingsScreen <--> SettingsViewModel
         └── HelpScreen
               │
[ Data Layer (Repositories & Utilities) ]
   ├── SafRepository          <--> Storage Access Framework (DocumentFile / Uri / Trash)
   ├── SettingsRepository     <--> DataStore Preferences
   ├── ExifReader             <--> ExifInterface (InputStream 解析)
   ├── ExifThumbnailFetcher   <--> Coil Custom Fetcher (EXIF サムネイル & 動画フレーム)
   ├── FileShareHelper        <--> FileProvider (ACTION_SEND 安全共有)
   └── MimeTypeUtils          <--> MIME タイプマッピング
```

---

## 📁 パッケージ・ディレクトリ構造 (Directory Structure)

```
app/src/main/java/com/example/photoselect/
├── MainActivity.kt                # 単一アクティビティ構成のエントリーポイント
├── data/
│   ├── model/
│   │   ├── MediaFile.kt           # 個別メディアファイル情報 (Uri, Name, Extension, Size, LastModified)
│   │   └── MediaGroup.kt          # ベース名でまとめたグループ (Primary, RAW, Video, Other)
│   ├── saf/
│   │   └── SafRepository.kt       # SAFツリースキャン・ペアリング処理・ファイル移動/コピー/ゴミ箱機能
│   ├── settings/
│   │   └── SettingsRepository.kt  # Preferences DataStore による拡張子・Tree URI 設定管理
│   └── exif/
│       ├── ExifMetadata.kt        # EXIFメタデータ保持用データクラス & 表示用フォーマッタ
│       ├── ExifReader.kt          # ExifInterfaceを用いたSAF InputStreamからの解析
│       └── ExifThumbnailFetcher.kt # Coil用カスタムFetcher（埋め込みEXIFサムネイル・動画フレームの高速抽出）
├── util/
│   ├── FileShareHelper.kt         # FileProvider経由での外部アプリ共有インテント生成
│   └── MimeTypeUtils.kt           # 拡張子ごとの MIME タイプ変換ユーティリティ
└── ui/
    ├── MainScreen.kt              # アプリ全体のアクティブ画面切り替え・バックスタックルーティング
    ├── gallery/
    │   ├── GalleryScreen.kt       # グリッド一覧表示・ナビゲーションドロワー・ゴミ箱画面・フィルタ/ソート
    │   └── GalleryViewModel.kt    # 一覧状態管理、プログレッシブ読込、一括選択・削除・移動・コピー
    ├── comparison/
    │   └── ComparisonScreen.kt    # 基準画像＋対象画像HorizontalPager・同期パン/ズーム・分割レイアウト切り替え
    ├── detail/
    │   ├── DetailScreen.kt        # フルスクリーン・HorizontalPager・ピンチズーム・動画外部再生
    │   └── ExifBottomSheet.kt     # EXIFメタデータオーバーレイシート
    ├── settings/
    │   ├── SettingsScreen.kt      # 拡張子カスタマイズ画面
    │   └── SettingsViewModel.kt   # 設定更新ViewModel
    ├── help/
    │   └── HelpScreen.kt          # ヘルプ＆アバウト画面、OSSライセンス表示
    ├── components/
    │   ├── BadgeChip.kt           # RAW / VIDEO 識別タグチップ
    │   └── MoveToTrashConfirmationDialog.kt # ゴミ箱移動確認ダイアログ
    └── theme/
        ├── Color.kt
        ├── Theme.kt               # Dynamic Color (Material 3) テーマ定義
        └── Type.kt
```

---

## 🛠️ 主要コンポーネントの仕様と実装詳細

### 1. SAFデータ層・マルチフォルダ & ゴミ箱機能 (`SafRepository` & `MediaGroup`)
- **ツリースキャン**: `DocumentFile.fromTreeUri` を使用して指定されたフォルダおよびサブフォルダ内の全ファイルを非同期スキャン。
- **ベース名でのペアリング**: ファイル名から拡張子を除いたベース文字列（例: `DSC0001`）を取得し、同名のプライマリ画像（JPEG/HEIF）、RAW画像（.RW2, .ARW等）、動画（.MP4, .MOV等）を単一の `MediaGroup` にカプセル化します。
- **プログレッシブバッチ読込**: 大量の画像が存在する場合、スキャン途中で30件ごとに段階的（バッチ）にUIへ反映させることで、初期表示の体感速度を大幅に向上させます。
- **マルチフォルダ権限管理**: ユーザーが追加した複数のルートフォルダの永続権限（`persistedUriPermissions`）を管理し、サイドナビゲーションドロワーから瞬時に切り替え可能。
- **ゴミ箱 (Trash) 機能**: ファイル削除時、ルートフォルダ配下に `Trash` ディレクトリを自動作成して一時退避。ゴミ箱画面からの個別に復元・完全消去、および一括ゴミ箱を空にする操作に対応。

### 2. 設定層 (`SettingsRepository` / DataStore)
- `androidx.datastore.preferences` を使用。
- ユーザー設定（プライマリ拡張子リスト、RAW拡張子リスト、現在選択されている Tree URI）を非同期Flowで提供。
- カンマ区切り文字列のパースおよびトリム、大文字変換処理をカプセル化。

### 3. EXIF解析 & 高速サムネイル描画 (`ExifReader`, `ExifMetadata`, `ExifThumbnailFetcher`)
- `context.contentResolver.openInputStream(uri)` でストリームを開き、`ExifInterface` 経由でメモリ効率よく解析。
- `TAG_MAKE`, `TAG_MODEL`, `TAG_LENS_MODEL`, `TAG_PHOTOGRAPHIC_SENSITIVITY`, `TAG_EXPOSURE_TIME`, `TAG_F_NUMBER`, `TAG_FOCAL_LENGTH`, `TAG_IMAGE_WIDTH`, `TAG_IMAGE_LENGTH` 等をパース。
- 分数表示シャッタースピード（例: `1/250s`）、絞り値（`f/2.8`）、ISO、焦点距離等の統合露出文字列を整形。
- **`ExifThumbnailFetcher`**: Coil 画像ライブラリにカスタム Fetcher を登録し、画像に内蔵された EXIF サムネイルビットマップまたは動画のファーストフレーム（`MediaMetadataRetriever`）を抽出し、フルサイズのデコードを避けて超高速にグリッド表示。

### 4. 動画ハンドリング (`DetailScreen` & `MediaMetadataRetriever`)
- ギャラリー一覧で動画（MP4/MOV/MKV等）を識別し、`VIDEO` バッジを表示。
- フルスクリーン詳細画面では `MediaMetadataRetriever` でファーストフレームをプレビュー表示し、システムインテント経由で外部動画プレーヤーアプリへ安全に渡して再生。

### 5. 外部共有パイプライン (`FileShareHelper` & `FileProvider`)
- Android 10+におけるセキュリティ要件に準拠し、`FileProvider` (`com.github.fujimotok.photoselect.fileprovider`) を使用。
- `res/xml/file_paths.xml` 内に `<cache-path name="shared_media" path="shared_media/" />` を登録。
- 共有実行時、対象ファイルを `cacheDir/shared_media/` に一時コピーし、`FileProvider.getUriForFile` で `content://` URIを発行。プライマリJPEGとペアRAWの個別の選択共有をサポート。

### 6. 同期ズーム＆パン比較画面 (`ComparisonScreen`)
- 基準画像（BASE）と、横スワイプで切り替え可能な比較対象画像（TARGET / HorizontalPager）を並列配置。
- 単一の `scale` および `offset` 状態変数を両方のパネルの `graphicsLayer` へ適用することで、完全同期した拡大・移動操作を実現。
- 画面回転に応じた自動レイアウト切り替えに加え、ユーザー指定による左右分割（Horizontal） / 上下分割（Vertical）の切り替えに対応。

---

## ⚙️ ビルドおよびテスト手順 (Build & Test)

### 1. デバッグビルドの実行
ターミナルにて以下のコマンドを実行してビルドの成功を確認します：

```bash
./gradlew assembleDebug
```

または Windows環境：
```cmd
gradlew.bat assembleDebug
```

### 2. ユニットテストの実行
モデル構造、選別ロジック、EXIF処理、フィルタ・ソート、ゴミ箱移動などのユニットテストを実行します：

```bash
./gradlew test
```

または：
```bash
./gradlew :app:testDebugUnitTest
```

---

## 🧪 テスト仕様 (Unit Tests)

- **`MediaGroupTest`**: RAWペア検出(`hasRaw`)、動画判定(`isVideo`)、拡張子初期設定の検証。
- **`GallerySelectionTest`**: 多重選択状態フラグ(`isSelectionMode`)、N枚選択ロジックの検証。
- **`GalleryFilterSortTest`**: 写真のみ/動画のみ/RAWありのフィルタリング処理および日付・名前・サイズソート処理の検証。
- **`GalleryTrashTest`**: ゴミ箱読み込み・復元・完全削除ロジックの検証。
- **`ExifMetadataTest`**: カメラ表示名フォーマット、露出パラメータ文字列、解像度文字列のフォーマット検証。
- **`ExifThumbnailTest`**: Coil サムネイル Fetcher のスキーム判定サポート検証。

