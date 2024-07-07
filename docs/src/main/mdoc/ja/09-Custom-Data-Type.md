# カスタム データ型

この章では、LDBCで構築したテーブル定義でユーザー独自の型もしくはサポートされていない型を使用するための方法について説明します。

@@@ vars
```yaml
version: '3'
services:
  mysql:
    image: mysql:"$mysqlVersion$"
    container_name: ldbc
    environment:
      MYSQL_USER: 'ldbc'
      MYSQL_PASSWORD: 'password'
      MYSQL_ROOT_PASSWORD: 'root'
    ports:
      - 13306:3306
    volumes:
      - ./database:/docker-entrypoint-initdb.d
    healthcheck:
      test: [ "CMD", "mysqladmin", "ping", "-h", "localhost" ]
      timeout: 20s
      retries: 10
```
@@@

次に、データベースの初期化を行います。以下のコードを使用して、データベースに接続し必要なテーブルを作成します。

@@snip [00-Setup.scala](/docs/src/main/scala/00-Setup.scala) { #setup }

**Scala CLIで実行**

このプログラムは、Scala CLIを使って実行することができる。以下のコマンドを実行すると、このプログラムを実行することができる。

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala
```

## ユーザー独自の型

ldbcではstatementに受け渡す値を`Parameter`で表現しています。`Parameter`はstatementへのバインドする値を表現するためのtraitです。

`Parameter`を実装することでstatementに受け渡す値をカスタム型で表現することができます。

以下のコード例では、`Parameter`を実装した`CustomParameter`を定義しています。

@@snip [05-Program.scala](/docs/src/main/scala/05-Program.scala) { #customType }

@@snip [05-Program.scala](/docs/src/main/scala/05-Program.scala) { #customParameter }

@@snip [05-Program.scala](/docs/src/main/scala/05-Program.scala) { #program1 }

これでstatementにカスタム型をバインドすることができるようになりました。

ldbcではパラメーターの他に実行結果から独自の型を取得するための`ResultSetReader`も提供しています。

`ResultSetReader`を実装することでstatementの実行結果から独自の型を取得することができます。

以下のコード例では、`ResultSetReader`を実装した`CustomResultSetReader`を定義しています。

@@snip [05-Program.scala](/docs/src/main/scala/05-Program.scala) { #customReader }

@@snip [05-Program.scala](/docs/src/main/scala/05-Program.scala) { #program2 }

これでstatementの実行結果からカスタム型を取得することができるようになりました。
