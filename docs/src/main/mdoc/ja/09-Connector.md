# コネクタ

この章では、LDBC独自のMySQLコネクタを使用したデータベース接続について説明します。

ScalaでMySQLデータベースへの接続を行うためにはJDBCを使用する必要があります。JDBCはJavaの標準ライブラリであり、Scalaでも使用することができます。
JDBCはJavaで実装が行われているためScalaで使用する場合でもJVM環境でのみ動作することができます。

昨今のScalaを取り巻く環境はJSやNativeなどの環境でも動作できるようプラグインの開発が盛んに行われています。
ScalaはJavaの資産を使用できるJVMのみで動作する言語から、クロスプラットフォーム環境でも動作できるよう進化を続けています。

しかし、JDBCはJavaの標準ライブラリでありScalaのクロスプラットフォーム環境での動作をサポートしていません。

そのため、ScalaでアプリケーションをJS, Nativeなどで動作できるように作成を行ったとしてもJDBCを使用できないため、MySQLなどのデータベースへ接続を行うことができません。

Typelevel Projectには[Skunk](https://github.com/typelevel/skunk)と呼ばれるPostgreSQL用のScalaライブラリが存在します。
このプロジェクトはJDBCを使用しておらず、純粋なScalaのみでPostgreSQLへの接続を実現しています。そのため、Skunkを使用すればJVM, JS, Native環境を問わずPostgreSQLへの接続を行うことができます。

LDBC コネクタはこのSkunkに影響を受けてJVM, JS, Native環境を問わずMySQLへの接続を行えるようにするために開発が行われてるプロジェクトです。

※ このコネクタは現在実験的な機能となります。そのため本番環境での使用には注意が必要です。

プロジェクトに以下の依存関係を設定する必要があります。

**JVM**

@@@ vars
```scala
libraryDependencies += "$org$" %% "ldbc-connector" % "$version$"
```
@@@

**JS/Native**

@@@ vars
```scala
libraryDependencies += "$org$" %%% "ldbc-connector" % "$version$"
```
@@@

コネクタは以下項目に分けて説明を行います。

- 認証
- 実行
- コネクションプーリング

## 認証

Comming soon...

## 実行

Comming soon...

## コネクションプーリング

Comming soon...
