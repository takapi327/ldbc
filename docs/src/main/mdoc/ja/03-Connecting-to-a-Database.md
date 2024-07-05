# データベースへの接続

この章では最初から始めます。まず、データベースに接続して値を返すプログラムを書き、そのプログラムをREPLで実行する。また、小さなプログラムを組み合わせてより大きなプログラムを構築することにも触れます。

## セットアップ

まず、`build.sbt`に依存関係を追加します。

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-dsl" % "$version$"
```
@@@

## 最初のプログラム

ldbcを使う前に、いくつかのシンボルをインポートする必要がある。ここでは便宜上、パッケージのインポートを使用する。これにより、高レベルAPIで作業する際に最もよく使用されるシンボルを得ることができる。

```scala
import ldbc.dsl.io.*
import ldbc.dsl.logging.LogHandler
```

Catsも連れてこよう。

```scala
import cats.syntax.all.*
import cats.effect.*
```

次に、トレーサーとログハンドラーを提供する。これらは、アプリケーションのログを記録するために使用される。トレーサーは、アプリケーションのトレースを記録するために使用される。ログハンドラーは、アプリケーションのログを記録するために使用される。

以下のコードは、トレーサーとログハンドラーを提供するがその実体は何もしない。

@@snip [01-Program.scala](/docs/src/main/scala/01-Program.scala) { #given }

ldbc高レベルAPIで扱う最も一般的な型はExecutor[F, A]という形式で、{java | ldbc}.sql.Connectionが利用可能なコンテキストで行われる計算を指定し、最終的にA型の値を生成します。

では、定数を返すだけのExecutorプログラムから始めてみよう。

@@snip [01-Program.scala](/docs/src/main/scala/01-Program.scala) { #program }

次に、データベースに接続するためのコネクタを作成する。コネクタは、データベースへの接続を管理するためのリソースである。コネクタは、データベースへの接続を開始し、クエリを実行し、接続を閉じるためのリソースを提供する。

@@snip [01-Program.scala](/docs/src/main/scala/01-Program.scala) { #connection }

Executorは、データベースへの接続方法、接続の受け渡し方法、接続のクリーンアップ方法を知っているデータ型であり、この知識によってExecutorをIOへ変換し、実行可能なプログラムを得ることができる。具体的には、実行するとデータベースに接続し、単一のトランザクションを実行するIOが得られる。

@@snip [01-Program.scala](/docs/src/main/scala/01-Program.scala) { #run }

万歳！定数を計算できた。これはデータベースに仕事を依頼することはないので、あまり面白いものではないが、最初の一歩が完了です。

> Keep in mind that all the code in this book is pure except the calls to IO.unsafeRunSync, which is the “end of the world” operation that typically appears only at your application’s entry points. In the REPL we use it to force a computation to “happen”.

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/01-Program.scala
```

## 2つめのプログラム

では、sql string interpolatorを使って、データベースに定数の計算を依頼する問い合わせを作成してみましょう。

@@snip [02-Program.scala](/docs/src/main/scala/02-Program.scala) { #program }

最後に、データベースに接続して値を返すプログラムを書く。このプログラムは、データベースに接続し、クエリを実行し、結果を取得する。

@@snip [02-Program.scala](/docs/src/main/scala/02-Program.scala) { #run }

定数を計算するためにデータベースに接続した。かなり印象的だ。

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/02-Program.scala
```

## 3つめのプログラム

一つの取引で複数のことをしたい場合はどうすればいいのか？簡単だ！Executorはモナドなので、for内包を使って2つの小さなプログラムを1つの大きなプログラムにすることができる。

@@snip [03-Program.scala](/docs/src/main/scala/03-Program.scala) { #program }

最後に、データベースに接続して値を返すプログラムを書く。このプログラムは、データベースに接続し、クエリを実行し、結果を取得する。

@@snip [03-Program.scala](/docs/src/main/scala/03-Program.scala) { #run }

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/03-Program.scala
```

## 4つめのプログラム

データベースに対して書き込みを行うプログラムを書いてみよう。ここでは、データベースに接続し、クエリを実行し、データを挿入する。

@@snip [04-Program.scala](/docs/src/main/scala/04-Program.scala) { #program }

先ほどと異なる点は、`commit`メソッドを呼び出すことである。これにより、トランザクションがコミットされ、データベースにデータが挿入される。

@@snip [04-Program.scala](/docs/src/main/scala/04-Program.scala) { #run }

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/04-Program.scala
```
