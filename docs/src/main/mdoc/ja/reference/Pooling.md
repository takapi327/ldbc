{%
laika.title = コネクションプーリング
laika.metadata.language = ja
%}

# コネクションプーリング

## 概要

ldbc-connectorは、高性能で安全なデータベースコネクションプーリングを提供する、Cats Effect向けに設計されたライブラリです。JVMの伝統的なスレッドベースのプーリング（HikariCPなど）とは異なり、Cats Effectのファイバーベースの並行性モデルを最大限に活用した設計となっています。

## アーキテクチャ概要

![Connection Pool Architecture](../../img/pooling/ConnectionPoolWithCircuitBreaker.svg)

ldbc-connectorのプーリングシステムは、以下の主要コンポーネントで構成されています：

### 1. PooledDataSource

プールの中核となるコンポーネントで、コネクションのライフサイクル全体を管理します。

主な責任：
- コネクションの取得と返却の調整
- プールサイズの管理
- メトリクスの収集
- バックグラウンドタスクの調整

### 2. ConcurrentBag

HikariCPのConcurrentBagからインスピレーションを受けた、高性能な並行データ構造です。ただし、JVMのスレッドではなくCats Effectのファイバー向けに最適化されています。

特徴：
- ロックフリーな操作
- ファイバー間での直接ハンドオフ
- 待機キューの効率的な管理
- `Ref[F]`を使用した原子的な状態管理

### 3. CircuitBreaker

データベースがダウンしている場合の「雷鳴の群れ（Thundering Herd）」問題を防ぐための重要なコンポーネントです。

## CircuitBreakerの詳細

### 状態遷移図

![Circuit Breaker State Transitions](../../img/pooling/CircuitBreakerStateTransition.svg)

### CircuitBreakerの目的

CircuitBreakerパターンは、以下の問題を解決するために実装されています：

1. **雷鳴の群れ（Thundering Herd）問題の防止**
   - データベースが一時的に利用できなくなった後、大量のクライアントが同時に再接続を試みる状況を防ぎます
   - これにより、既に問題を抱えているデータベースへの負荷をさらに増加させることを避けられます

2. **フェイルファスト**
   - データベースが応答しない場合、新しい接続試行を即座に失敗させます
   - これにより、クライアントは長いタイムアウトを待つ必要がなくなります

3. **段階的な回復**
   - Half-Open状態を通じて、サービスが回復したかどうかを慎重にテストします
   - 指数バックオフにより、繰り返し失敗する場合の再試行間隔を徐々に増やします

### 実装の詳細

```scala
trait CircuitBreaker[F[_]]:
  def protect[A](action: F[A]): F[A]
  def state: F[CircuitBreaker.State]
  def reset: F[Unit]
```

設定パラメータ：
- `maxFailures`: Open状態に移行するまでの失敗回数（デフォルト: 5）
- `resetTimeout`: Open状態から Half-Open状態への移行までの時間（デフォルト: 60秒）
- `exponentialBackoffFactor`: 失敗時のタイムアウト増加係数（デフォルト: 2.0）
- `maxResetTimeout`: 最大リセットタイムアウト（デフォルト: 5分）

### 動作フロー

1. **Closed（閉）状態**
   - すべてのリクエストが通常通り処理されます
   - 失敗がカウントされ、閾値に達するとOpen状態に移行します

2. **Open（開）状態**
   - すべてのリクエストが即座に失敗します（フェイルファスト）
   - リセットタイムアウト経過後、Half-Open状態への移行を試みます

3. **Half-Open（半開）状態**
   - 単一のテストリクエストを許可します
   - 成功した場合：Closed状態に戻ります
   - 失敗した場合：Open状態に戻り、タイムアウトを指数的に増加させます

## JVMスレッド vs Cats Effectファイバー

### なぜファイバーベースの設計が優れているのか

![Threads vs Fibers Comparison](../../img/pooling/ThreadsVsFibers.svg)

#### JVMスレッドの特性（HikariCPなどの従来型プール）

- **メモリ使用量**: スレッドあたり1-2MB
- **コンテキストスイッチ**: カーネルレベルで高コスト
- **スケーラビリティ**: 数千スレッドが実用的な上限
- **ブロッキング**: スレッドが実際にブロックされ、リソースを無駄にする

#### Cats Effectファイバーの特性（ldbc-connector）

- **メモリ使用量**: ファイバーあたり約150バイト
- **コンテキストスイッチ**: ほぼゼロコスト
- **スケーラビリティ**: 数百万のファイバーが可能
- **ブロッキング**: セマンティックブロッキング（ワーカースレッドは解放される）

### プーリングへの影響

この根本的な違いが、ldbc-connectorの設計に以下の影響を与えています：

1. **大量の並行リクエストの処理**
   - 従来のプール：スレッド枯渇により制限
   - ldbc-connector：ファイバーは軽量なため、数千の並行リクエストを処理可能

2. **待機戦略**
   - 従来のプール：ブロッキング待機がスレッドを消費
   - ldbc-connector：セマンティックブロッキングにより、待機中もワーカースレッドは他の作業を実行可能

3. **リソース効率**
   - 従来のプール：アイドル状態のコネクションがスレッドを保持
   - ldbc-connector：アイドル状態のコネクションはメモリのみを消費

## HikariCPとの比較

### 共通点

- 高性能を重視した設計
- ConcurrentBagのようなロックフリーなデータ構造
- プロキシパターンによるコネクション管理
- 自動的なプールサイズ調整

### 相違点

| 特徴 | HikariCP | ldbc-connector |
|------|----------|----------------|
| 並行性モデル | JVMスレッド | Cats Effectファイバー |
| ブロッキング処理 | スレッドをブロック | セマンティックブロッキング |
| スケーラビリティ | スレッド数に制限 | ファイバー数はほぼ無制限 |
| CircuitBreaker | 外部ライブラリが必要 | 組み込み |
| エラー処理 | 例外ベース | 関数型（F[_]） |
| リソース管理 | try-with-resources | Resource[F, _] |

### パフォーマンス特性

ldbc-connectorは、特に以下のシナリオで優れたパフォーマンスを発揮します：

1. **高並行性環境**
   - 数千の同時接続リクエスト
   - マイクロサービスアーキテクチャ
   - リアクティブアプリケーション

2. **I/Oバウンドなワークロード**
   - 長時間実行されるクエリ
   - 複数のデータベースへの同時アクセス
   - 非同期処理パイプライン

3. **障害回復シナリオ**
   - CircuitBreakerによる迅速なフェイルオーバー
   - 雷鳴の群れ問題の防止
   - 段階的な回復メカニズム

## バックグラウンドタスク

ldbc-connectorは、プールの健全性を維持するために複数のバックグラウンドタスクを実行します：

### HouseKeeper
- 期限切れコネクションの削除
- アイドルタイムアウトの処理
- 最小コネクション数の維持

### AdaptivePoolSizer
- 使用率メトリクスに基づく動的なプールサイズ調整
- 負荷に応じた拡張と縮小
- クールダウン期間による安定化

### KeepaliveExecutor
- アイドルコネクションの定期的な検証
- 接続の維持とタイムアウトの防止

## 設定例

```scala
import ldbc.connector.ConnectionPoolConfig

val config = ConnectionPoolConfig(
  maxPoolSize = 10,
  minIdle = 2,
  connectionTimeout = 30.seconds,
  idleTimeout = 10.minutes,
  maxLifetime = 30.minutes,
  validationTimeout = 5.seconds,
  leakDetectionThreshold = Some(2.minutes),
  
  // CircuitBreaker設定
  circuitBreakerConfig = CircuitBreaker.Config(
    maxFailures = 5,
    resetTimeout = 60.seconds,
    exponentialBackoffFactor = 2.0,
    maxResetTimeout = 5.minutes
  )
)
```

## まとめ

ldbc-connectorのプーリングシステムは、Cats Effectの強力な並行性モデルを活用することで、従来のJVMベースのプーリングソリューションを超える性能と安全性を提供します。CircuitBreakerパターンの組み込みにより、データベース障害時の回復力も大幅に向上しています。

ファイバーベースのアーキテクチャにより、少ないリソースでより多くの並行接続を処理でき、現代のクラウドネイティブアプリケーションやマイクロサービスアーキテクチャに最適なソリューションとなっています。
