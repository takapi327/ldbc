{%
laika.title = パフォーマンス
laika.metadata.language = ja
%}

# パフォーマンス

本ドキュメントでは、ldbcとjdbcのパフォーマンス特性を詳細に分析し、どのような状況でldbcを選択すべきかについて技術的な観点から解説します。

## エグゼクティブサマリー

ベンチマーク結果から、ldbcはjdbcと比較して約1.8〜2.1倍の高いスループットを示しています。この優位性は、Cats EffectのFiberベースの並行性モデルと非ブロッキングI/Oの実装に起因します。特に高並行性環境において、ldbcは優れたスケーラビリティを発揮します。

### 主要な発見事項

1. **パフォーマンス**: ldbcはjdbcより約2倍高速（8スレッド環境）
2. **スケーラビリティ**: スレッド数の増加に対してldbcは線形に近いスケーリングを実現
3. **リソース効率**: メモリ使用量が大幅に削減（Fiber: 500バイト vs OSスレッド: 1MB）
4. **レイテンシ**: 高負荷時でも安定した応答時間を維持

## スレッド数によるパフォーマンス比較

### 1スレッド環境

@:image(/img/connector/Select_Thread1.svg) {
alt = "Select Benchmark (1 Thread)"
}

シングルスレッド環境では、ldbcとjdbcのパフォーマンス差は比較的小さくなります。これは、並行性の利点が発揮されないためです。

**パフォーマンス比率（ldbc/jdbc）**:
- 500行: 1.43倍
- 1000行: 1.52倍
- 1500行: 1.48倍
- 2000行: 1.51倍

### 2スレッド環境

@:image(/img/connector/Select_Thread2.svg) {
alt = "Select Benchmark (2 Threads)"
}

2スレッド環境から、ldbcの優位性が明確になり始めます。

**パフォーマンス比率（ldbc/jdbc）**:
- 500行: 1.83倍
- 1000行: 1.48倍
- 1500行: 1.66倍
- 2000行: 1.75倍

### 4スレッド環境

@:image(/img/connector/Select_Thread4.svg) {
alt = "Select Benchmark (4 Threads)"
}

4スレッド環境では、ldbcのスケーラビリティが顕著に現れます。

**パフォーマンス比率（ldbc/jdbc）**:
- 500行: 1.89倍
- 1000行: 1.82倍
- 1500行: 1.87倍
- 2000行: 1.93倍

### 8スレッド環境

@:image(/img/connector/Select_Thread8.svg) {
alt = "Select Benchmark (8 Threads)"
}

8スレッド環境で、ldbcは最も高いパフォーマンス優位性を示します。

**パフォーマンス比率（ldbc/jdbc）**:
- 500行: 1.76倍
- 1000行: 2.01倍
- 1500行: 1.92倍
- 2000行: 2.09倍

### 16スレッド環境

@:image(/img/connector/Select_Thread16.svg) {
alt = "Select Benchmark (16 Threads)"
}

16スレッド環境でも、ldbcは安定した高パフォーマンスを維持します。

**パフォーマンス比率（ldbc/jdbc）**:
- 500行: 1.95倍
- 1000行: 2.03倍
- 1500行: 1.98倍
- 2000行: 2.12倍

## 技術的分析

### 並行性モデルの違い

#### ldbc（Cats Effect 3）

ldbcはCats Effect 3のFiberベースの並行性モデルを採用しています：

```scala
// 非ブロッキングI/O操作
for {
  statement <- connection.prepareStatement(sql)
  _         <- statement.setInt(1, id)
  resultSet <- statement.executeQuery()
  result    <- resultSet.decode[User]
} yield result
```

**特徴**:
- **Fiber（グリーンスレッド）**: 軽量なユーザー空間スレッド
  - メモリ使用量: 約300-500バイト/Fiber
  - コンテキストスイッチ: ユーザー空間で完結（カーネル呼び出し不要）
  - CPUキャッシュ効率: スレッドアフィニティによる高いキャッシュヒット率

- **Work-Stealingスレッドプール**:
  - CPUコア毎の作業キュー（グローバル競合を回避）
  - 動的負荷分散
  - 自動yield挿入によるCPU飢餓防止

#### jdbc（従来のスレッドモデル）

jdbcは従来のOSスレッドとブロッキングI/Oを使用：

```scala
// ブロッキングI/O操作
Sync[F].blocking {
  val statement = connection.prepareStatement(sql)
  statement.setInt(1, id)
  val resultSet = statement.executeQuery()
  // スレッドがブロックされる
}
```

**特徴**:
- **OSスレッド**: ネイティブスレッド
  - メモリ使用量: 約1MB/スレッド
  - コンテキストスイッチ: カーネル呼び出しが必要
  - 固定サイズスレッドプール

### ネットワークI/O実装

#### ldbc - 非ブロッキングソケット

```scala
// fs2 Socketを使用した非ブロッキング読み取り
socket.read(8192).flatMap { chunk =>
  // チャンク単位での効率的な処理
  processChunk(chunk)
}
```

- **ゼロコピー最適化**: BitVectorによる効率的なバッファ管理
- **ストリーミング**: 大きな結果セットの効率的な処理
- **タイムアウト制御**: 細粒度のタイムアウト設定が可能

#### jdbc - ブロッキングソケット

```scala
// 従来のブロッキングI/O
val bytes = inputStream.read(buffer)
// スレッドがI/O完了までブロック
```

- **バッファリング**: 結果セット全体をメモリに読み込み
- **スレッドブロッキング**: I/O待機中はスレッドが使用不可

### メモリ効率とGC圧力

#### ldbcのメモリ管理

1. **プリアロケートバッファ**: 結果行用の再利用可能なバッファ
2. **ストリーミング処理**: 必要に応じたデータフェッチ
3. **不変データ構造**: 構造共有による効率的なメモリ使用

#### jdbcのメモリ管理

1. **一括読み込み**: 結果セット全体をメモリに保持
2. **中間オブジェクト**: ボクシング/アンボクシングによるオーバーヘッド
3. **GC圧力**: 一時オブジェクトによる頻繁なGC

## 使用シナリオ別推奨事項

### ldbcを選択すべきケース

1. **高並行性アプリケーション**
   - Webアプリケーション（高トラフィック）
   - マイクロサービス
   - リアルタイムデータ処理

2. **リソース制約環境**
   - コンテナ環境（Kubernetes等）
   - サーバーレス環境
   - メモリ制限のある環境

3. **スケーラビリティ重視**
   - 将来的な負荷増加が予想される
   - 弾力的なスケーリングが必要
   - クラウドネイティブアプリケーション

4. **関数型プログラミング**
   - 純粋関数型アーキテクチャ
   - 型安全性重視
   - コンポーザビリティ重視

### jdbcを選択すべきケース

1. **レガシーシステム統合**
   - 既存のjdbcコードベース
   - サードパーティライブラリ依存
   - 移行コストが高い場合

2. **シンプルなCRUD操作**
   - 低並行性
   - バッチ処理
   - 管理ツール

3. **特殊なjdbc機能**
   - ベンダー固有の拡張機能
   - 特殊なドライバー要件

## パフォーマンスチューニング

### ldbc最適化設定

```scala
val datasource = MySQLDataSource
  .build[IO]("localhost", 3306, "user")
  .setPassword("password")
  .setDatabase("db")
  // パフォーマンス設定
  .setUseServerPrepStmts(true)      // サーバーサイドプリペアドステートメント
  .setUseCursorFetch(true)           // カーソルベースフェッチング
  .setFetchSize(1000)                // フェッチサイズ
  .setSocketOptions(List(
    SocketOption.noDelay(true),      // TCP_NODELAY
    SocketOption.keepAlive(true)     // キープアライブ
  ))
  .setReadTimeout(30.seconds)        // 読み取りタイムアウト
```

### スレッドプール設定

```scala
// Cats Effect 3のランタイム設定
object Main extends IOApp {
  override def computeWorkerThreadCount: Int = 
    math.max(4, Runtime.getRuntime.availableProcessors())
    
  override def run(args: List[String]): IO[ExitCode] = {
    // アプリケーションロジック
  }
}
```

## まとめ

ldbcは、特に以下の条件を満たす場合に優れた選択肢となります：

1. **高並行性**: 多数の同時接続を処理する必要がある
2. **スケーラビリティ**: 負荷に応じた柔軟なスケーリングが必要
3. **リソース効率**: メモリ使用量を最小限に抑えたい
4. **型安全性**: コンパイル時の型チェックを重視
5. **関数型プログラミング**: 純粋関数型のアーキテクチャを採用

ベンチマーク結果が示すように、ldbcは8スレッド以上の環境でjdbcの約2倍のスループットを実現し、さらに高いスレッド数でも性能劣化が少ない優れたスケーラビリティを持っています。現代的なクラウドネイティブアプリケーションや高トラフィックWebサービスにおいて、ldbcは強力な選択肢となるでしょう。