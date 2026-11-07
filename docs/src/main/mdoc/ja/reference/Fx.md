{%
laika.title = Fx
laika.metadata.language = ja
%}

# Fx

## 概要

`ldbc-fx`は、外部のエフェクトライブラリに依存しない軽量なエフェクト型`Fx`と、その実行基盤`FxRuntime`を提供するモジュールです。

0.9.x のドライバ・ネットワーク層・コネクションプールは`ldbc-effect`の型クラス（`Async ⊂ Temporal ⊂ Concurrent`）に対して書かれており、`Concurrent`インスタンスを持つエフェクトであればネイティブに動きます。`Fx`はその実装の 1 つです。

@:callout(info)

**ほとんどの利用者はこのページを読む必要がありません**

Cats Effect（`IO`）や ZIO（`Task`）を使う場合、`Fx`は登場しません。`Fx`が関係するのは次の 2 つの場合です。

- **`scala.concurrent.Future`を使う場合**: `Future`はキャンセルと fork を持たないため`Concurrent`を満たせません。そのため`ldbc-future`は内部で`Fx`上でドライバを動かし、実行 1 回につき 1 回だけ`Future`へ変換します。この場合も`Fx`を直接書く必要はありません
- **エフェクトライブラリを一切使わない場合**: `Fx`を直接扱えます

@:@

## Future バックエンドとしての利用

`ldbc-future`を使う場合、`Fx`は`MySQLDataSource`の型パラメータとしてのみ現れます。

```scala
import scala.concurrent.Future

import ldbc.dsl.*
import ldbc.fx.{ Fx, concurrentFx }
import ldbc.future.Connector
import ldbc.mysql.MySQLDataSource
import ldbc.net.SSL

val datasource = MySQLDataSource
  .build[Fx]("127.0.0.1", 3306, "ldbc")
  .setPassword("password")
  .setDatabase("world")
  .setSSL(SSL.Trusted)

val connector = Connector.fromDataSource(datasource)

val cities: Future[List[String]] =
  sql"SELECT Name FROM city".query[String].to[List].readOnly(connector)
```

`Future`にはホストランタイムが無く差し替える理由も無いため、この経路の`Fx`プログラムは常にプラットフォーム既定の`FxRuntime.global`上で実行されます。

## エフェクト型 `Fx`

`Fx`は遅延・キャンセル可能・スタックセーフな非同期エフェクトです。副作用は`unsafeRun`を呼ぶまで実行されません。

### 生成

| 構築子 | 意味 |
|---|---|
| `Fx.pure(a)` | 計算済みの値 |
| `Fx.unit` | `()` を返す |
| `Fx.raiseError(t)` | 失敗する |
| `Fx.delay(thunk)` | 同期的な副作用を遅延させる。呼び出しスレッドで実行される |
| `Fx.blocking(thunk)` | ブロックする処理を専用プールで実行する（JS ではインライン実行） |
| `Fx.interruptible(thunk)` | `blocking`と同様。ただしキャンセル時に`Thread.interrupt()`される |
| `Fx.async(k)` | コールバック API を`Fx`に橋渡しする。`k`は完了コールバックを登録し`Canceler`を返す |
| `Fx.never` | 決して完了しない |
| `Fx.sleep(d)` | 指定時間後に完了する |

`blocking`と`interruptible`の違いは、キャンセル時に thunk 自体が中断されるかどうかです。`blocking`は境界でキャンセル可能（結果が捨てられ継続が動かない）ですが、thunk は最後まで走ります。

### 合成

```scala
import ldbc.fx.syntax.*

val program: Fx[Int] =
  Fx.delay(1)
    .flatMap(n => Fx.delay(n + 1))
    .map(_ * 10)
    .handleErrorWith(_ => Fx.pure(0))
```

`ldbc.fx.syntax.*`をインポートすると`>>` `*>` `<*` `void` `as` `flatTap` `attempt` `handleError` `guarantee` `onError` `start` `timeout` が使えます。コレクションに対しては `traverse` `traverse_` `filterA` `parTraverse` `parTraverse_` `parTraverseN` が、タプルに対しては `parTupled` が使えます。

### リソースとキャンセル

```scala
Fx.bracket(acquire)(use)(release)
```

`release`は成功・失敗・キャンセルのいずれでも実行されます。`acquire`はマスクされるため、獲得中のキャンセルでリソースが漏れることはありません。

| API | 意味 |
|---|---|
| `Fx.bracket(acquire)(use)(release)` | 獲得・使用・解放 |
| `Fx.uncancelable(body)` | `body`の間キャンセルを保留する。部分的な`poll`は無い |
| `fa.onCancel(fin)` | **キャンセル時のみ** `fin`を実行する。成功・失敗では実行されない |
| `fa.guarantee(fin)` | 成功・失敗・キャンセルのいずれでも`fin`を実行する |

### 実行

```scala
val canceler: Fx.Canceler = program.unsafeRun {
  case Right(value) => println(value)
  case Left(error)  => error.printStackTrace()
}
```

`unsafeRun`は副作用が実際に起きる境界です。戻り値の`Canceler`で中断を要求できます。

`unsafeRunCancelable`は`Canceler`の代わりに`CancelToken`を返します。こちらの`cancel: Fx[Unit]`は、**キャンセル経路の finalizer が実際に流れ終わってから**完了します。ロールバックやリソース解放の完了を待ちたい場合に使います。

## 並行プリミティブ

| 型 | 生成 | 用途 |
|---|---|---|
| `Ref[A]` | `Ref.of(a)` / `Ref.unsafe(a)` | アトミックに更新できる可変参照。`get` `set` `update` `modify` ほか |
| `Deferred[A]` | `Deferred[A]` | 一度だけ書ける非同期の値。`get`は完了までサスペンドする |
| `Semaphore` | `Semaphore(permits)` | FIFO のカウンティングセマフォ。`withPermit`で囲む |
| `Mutex` | `Mutex.create` | 排他制御。`surround`で囲む |
| `Resource[A]` | `Resource.make` / `eval` / `pure` | 獲得・解放の組。`use`で消費し、`flatMap`で入れ子にすると LIFO で解放される |
| `Fiber[A]` | `fa.start` | 並行実行。`join`は`Outcome`を返し、`joinWithNever` / `joinWith` / `cancel`もある |

`Fiber#join`が返す`Outcome`は`Succeeded(a)` / `Errored(e)` / `Canceled` の 3 つです。

## FxRuntime

`FxRuntime`は`Fx`の実行基盤です。run loop がどこに処理をスケジュールするかを決めます。

```scala
trait FxRuntime:
  def executeCompute(task: () => Unit): Unit
  def executeBlocking(task: () => Unit): Unit
  def executeInterruptible(task: () => Unit): Fx.Canceler
  def scheduleOnce(delayNanos: Long, task: () => Unit): Fx.Canceler

  def autoCedeThreshold: Int = FxRuntime.defaultAutoCedeThreshold
  def finalizerTimeout: FiniteDuration = FxRuntime.defaultFinalizerTimeout
```

### プラットフォーム既定のランタイム

`FxRuntime.global`はプラットフォームごとに用意されています。プールはいずれも遅延生成されるため、使わなければコストはかかりません。

| プラットフォーム | compute | blocking | scheduler |
|---|---|---|---|
| JVM / Native | 固定サイズプール（コア数、最小 2）`fx-compute` | キャッシュプール `fx-blocking` | 単一スレッド `fx-scheduler` |
| Scala.js | `setTimeout(0)` でイベントループに譲る | インライン実行（オフロード先が無い） | `setTimeout` |

スレッドはすべてデーモンです。

### どのランタイムで動くか

`unsafeRun`は`using rt: FxRuntime = FxRuntime.current`を取ります。`FxRuntime.current`はそのスレッドで有効なランタイム、無ければ`global`です。解釈中に開始された入れ子の`unsafeRun`は、外側のランタイムを引き継ぎます。

明示的に指定する場合は次のように書きます。

```scala
program.unsafeRun(callback)(using myRuntime)
```

### チューニング

`autoCedeThreshold`と`finalizerTimeout`は既定実装を持つので、必要なものだけ override すれば足ります。

```scala
final class LatencySensitiveRuntime(delegate: FxRuntime) extends FxRuntime:
  export delegate.{ executeCompute, executeBlocking, executeInterruptible, scheduleOnce }

  override def autoCedeThreshold: Int            = 64
  override def finalizerTimeout:  FiniteDuration = 5.seconds

val runtime: FxRuntime = new LatencySensitiveRuntime(FxRuntime.global)

program.unsafeRun(callback)(using runtime)
```

`export`の対象を`FxRuntime.global`ではなくコンストラクタ引数の`delegate`にしているのは、`export`が不変のパス（immutable path）を要求するためです。`FxRuntime.global`は`def`なので直接は`export`できません。

**`autoCedeThreshold`** — 連続した同期ステップが何回続いたら継続を`executeCompute`へ載せ替えてスレッドを解放するかの閾値です。既定は 1024。

長い同期チェーンが、継続をインラインで再開する I/O ポーラーやセレクタのスレッドを占有するのを防ぎます。レイテンシに敏感なスレッドを持つランタイムでは小さく、計算専用のランタイムでは大きくします。

@:callout(warning)

**`FxRuntime.minAutoCedeThreshold`（= 2）以上である必要があります。**

1 以下だと、1 ステップも実行しないうちに cede 判定が成立し、run loop が進行しないまま再スケジュールを繰り返します。run loop はこの下限でクランプするため、範囲外の値を指定してもハングはせず「頻繁に cede する」だけになりますが、意図した値にはなりません。

@:@

**`finalizerTimeout`** — キャンセル経路の release 1 本あたりの上限です。既定は 30 秒。

死んだ接続へのロールバックのように、決して完了しない release があっても`CancelToken.cancel`が永久にハングしないようにします。適切な値はそのランタイムで動くプログラムが何を解放するかによるため、プロセス全体ではなくランタイムごとの値になっています。

@:callout(warning)

**正の値である必要があります。**

0 以下を指定すると、すべての release が実行前に打ち切られます。release のエラーは握り潰されるため、解放されるはずだったリソースが黙って解放されなくなります。なお「無制限に待つ」は表現できません。この上限は`cancel`のハングを防ぐために存在するためです。

@:@

## 他のエフェクトとの関係

`ldbc.fx.concurrentFx`が`ldbc.effect.Concurrent[Fx]`のインスタンスです。これにより、`F: Concurrent`に対して書かれたドライバ・ネットワーク層・プールがそのまま`Fx`で動きます。

`Fx`と cats の橋渡し（`cats.MonadError[Fx, Throwable]`）は`ldbc-future`の`FxInstances`にあります。`ldbc-fx`自体は cats に依存しません。DB 非依存のコアをエフェクト非依存に保つため、依存を利用する側へ寄せています。
