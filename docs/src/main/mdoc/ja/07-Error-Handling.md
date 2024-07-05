# エラーハンドリング

この章では、例外をトラップしたり処理したりするプログラムを構築するためのコンビネーター一式を検討する。

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

```shell
scala-cli https://github.com/takapi327/ldbc/tree/master/docs/src/main/scala/00-Setup.scala
```

## 例外について

ある操作が成功するかどうかは、ネットワークの健全性、テーブルの現在の内容、ロックの状態など、予測できない要因に依存します。そのため、EitherT[Executor, Throwable, A]のような論理和ですべてを計算するか、明示的に捕捉されるまで例外の伝播を許可するかを決めなければならない。つまり、ldbcのアクション（ターゲット・モナドに変換される）が実行されると、例外が発生する可能性がある。

発生しやすい例外は主に3種類ある

1. あらゆる種類のI/Oで様々なタイプのIOExceptionが発生する可能性があり、これらの例外は回復できない傾向がある。
2. データベース例外は、通常、ベンダー固有のSQLStateで特定のエラーを識別する一般的なSQLExceptionとして、キー違反のような一般的な状況で発生します。エラーコードは伝承として伝えられるか、実験によって発見されなければなりません。XOPENとSQL:2003の標準がありますが、どのベンダーもこれらの仕様に忠実ではないようです。これらのエラーには回復可能なものとそうでないものがある。
3. ldbcは、無効な型マッピング、ドライバから返される未知の JDBC 定数、観測される NULL 値、その他 ldbc が想定している不変条件の違反に対して InvariantViolation を発生させます。これらの例外はプログラマのエラーかドライバの不適合を示し、一般に回復不可能です。

