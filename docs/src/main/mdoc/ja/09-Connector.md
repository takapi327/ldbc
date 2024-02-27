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

MySQLでの認証は、クライアントがMySQLサーバーへ接続するときにLoginRequestというフェーズでユーザ情報を送信します。そして、サーバー側では送られたユーザが`mysql.user`テーブルに存在するか検索を行いどの認証プラグインを使用するか決定します。認証プラグインが決定した後にサーバーはそのプラグインを呼び出してユーザー認証を開始し、その結果をクライアント側に送信します。このようにMySQLでは認証がプラガブル（様々なタイプのプラグインを付け外しできる）になっています。

MySQLでサポートされている認証プラグインは[公式ページ](https://dev.mysql.com/doc/refman/8.0/ja/authentication-plugins.html)に記載されています。

LDBCは現時点で以下の認証プラグインをサポートしています。

- ネイティブプラガブル認証
- SHA-256 プラガブル認証
- SHA-2 プラガブル認証のキャッシュ

※ ネイティブプラガブル認証とSHA-256 プラガブル認証はMySQL 8.xから非推奨となったプラグインです。特段理由がない場合はSHA-2 プラガブル認証のキャッシュを使用することを推奨します。

## 実行

Comming soon...

## コネクションプーリング

Comming soon...
