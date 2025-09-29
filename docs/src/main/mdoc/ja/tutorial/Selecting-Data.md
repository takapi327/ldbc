{%
  laika.title = データ選択
  laika.metadata.language = ja
%}

# データ選択

[パラメータ化クエリ](/ja/tutorial/Parameterized-Queries.md)の使い方を学んだところで、今度はさまざまな形式でデータを取得する方法を見ていきましょう。このページでは、SELECTクエリを使ってデータを効率的に取得し、Scalaの型にマッピングする方法を説明します。

ldbcの最も強力な機能の1つは、データベースの結果をScalaの型に簡単にマッピングできることです。単純なプリミティブ型から複雑なケースクラスまで、さまざまなデータ形式を扱えます。

※ このチュートリアルでは、データベース操作を実行するために`Connector`を使用します。以下のように作成します：

```scala
import ldbc.connector.*

// Connectorを作成
val connector = Connector.fromDataSource(datasource)
```

## データ取得の基本ワークフロー

ldbcでデータを取得する基本的な流れは以下の通りです：

1. SQLクエリを`sql`補間子で作成
2. `.query[T]`で結果の型を指定
3. `.to[Collection]`で結果をコレクションに変換（オプション）
4. `.readOnly(connector)`/`.commit(connector)`/`.transaction(connector)`などでクエリを実行
5. 結果を処理

この流れをコード上の型の変化と共に見ていきましょう。

## コレクションへの行の読み込み

最初のクエリでは、いくつかのユーザー名をリストに取得し、出力する例を見てみましょう。各ステップで型がどう変化するかを示しています：

```scala
sql"SELECT name FROM user"
  .query[String]                 // Query[String]
  .to[List]                      // DBIO[List[String]]
  .readOnly(connector)           // IO[List[String]]
  .unsafeRunSync()               // List[String]
  .foreach(println)              // Unit
```

このコードを詳しく説明すると：

- `sql"SELECT name FROM user"` - SQLクエリを定義します。
- `.query[String]` - 各行の結果を`String`型にマッピングします。これにより`Query[String]`型が生成されます。
- `.to[List]` - 結果を`List`に集約します。`DBIO[List[String]]`型が生成されます。このメソッドは`FactoryCompat`を実装する任意のコレクション型（`List`、`Vector`、`Set`など）で使用できます。似たような方法として以下があります。
    - `.unsafe`は単一の値を返し、正確に1行でない場合は例外を発生させる。
    - `.option`は単一の値をOptionに包んで返し、返される行が複数ある場合は例外を発生させる。
    - `.nel`は複数の値をNonEmptyListに包んで返し、返される行がない場合は例外を発生させます。
- `.readOnly(connector)` - コネクションを読み取り専用モードで使用してクエリを実行します。戻り値は`IO[List[String]]`です。
- `.unsafeRunSync()` - IOモナドを実行して実際の結果（`List[String]`）を取得します。
- `.foreach(println)` - 結果の各要素を出力します。

## 複数列クエリ

もちろん、複数のカラムを選択してタプルにマッピングすることもできます：

```scala
sql"SELECT name, email FROM user"
  .query[(String, String)]       // Query[(String, String)]
  .to[List]                      // DBIO[List[(String, String)]]
  .readOnly(connector)                // IO[List[(String, String)]]
  .unsafeRunSync()               // List[(String, String)]
  .foreach { case (name, email) => println(s"Name: $name, Email: $email") }
```

複数列クエリでは、選択したカラムの順序とタプルの型パラメータの順序が一致していることが重要です。上記の例では、`name`が1番目のカラム（タプルの`_1`）、`email`が2番目のカラム（タプルの`_2`）に対応します。

## ケースクラスへのマッピング

タプルは便利ですが、コードの可読性を高めるためにケースクラスを使用することをお勧めします。ldbcは、クエリの結果を自動的にケースクラスにマッピングできます：

```scala
// ユーザー情報を表すケースクラス
case class User(id: Long, name: String, email: String)

// クエリ実行とマッピング
sql"SELECT id, name, email FROM user"
  .query[User]                   // Query[User]
  .to[List]                      // DBIO[List[User]]
  .readOnly(connector)                // IO[List[User]]
  .unsafeRunSync()               // List[User]
  .foreach(user => println(s"ID: ${user.id}, Name: ${user.name}, Email: ${user.email}"))
```

### マッピングの仕組み

以下の図は、SQLクエリの結果がどのようにしてUserモデルにマッピングされるかを示しています：

![シンプルなマッピングの仕組み](../../../img/select-mapping-simple-JP.svg)

この図から以下のような形でマッピング処理が行われています：

1. **SQL実行**: `sql"SELECT id, name, email FROM user"`文字列補間でクエリを作成し、`.query[User]`で結果の型を指定
2. **ResultSet**: データベースから返される結果セット（カラム番号1から順に値が格納）
3. **Decoder解決**: コンパイル時に`Decoder[Long]`、`Decoder[String]`などの基本Decoderを合成して`Decoder[User]`を構築
4. **マッピング処理**: 実行時に各カラムの値をdecodeメソッドで適切な型に変換し、最終的にUserインスタンスを生成

## 複数テーブルの結合とネストしたケースクラス

`JOIN`を使って複数のテーブルからデータを取得する場合、ネストしたケースクラス構造にマッピングすることができます。以下の例では、`city`テーブルと`country`テーブルを結合し、結果を`CityWithCountry`クラスにマッピングしています：

```scala
// 都市を表すケースクラス
case class City(id: Long, name: String)

// 国を表すケースクラス
case class Country(code: String, name: String, region: String)

// 都市と国の情報を組み合わせたケースクラス
case class CityWithCountry(city: City, country: Country)

// 結合クエリの実行
sql"""
  SELECT
    city.id,
    city.name,
    country.code,
    country.name,
    country.region
  FROM city
  JOIN country ON city.country_code = country.code
"""
  .query[CityWithCountry]        // Query[CityWithCountry]
  .to[List]                      // DBIO[List[CityWithCountry]]
  .readOnly(connector)                // IO[List[CityWithCountry]]
  .unsafeRunSync()               // List[CityWithCountry]
  .foreach(cityWithCountry => println(
    s"City: ${cityWithCountry.city.name}, Country: ${cityWithCountry.country.name}"
  ))
```

### 複雑なマッピングの仕組み

以下の図は、JOIN結果がどのようにしてネストしたケースクラス（CityWithCountry）にマッピングされるかを示しています：

![複雑なマッピングの仕組み（JOIN結果）](../../../img/select-mapping-complex-JP.svg)

この図から分かるように：

1. **SQL実行**: JOIN句を含むSQLクエリを実行し、`.query[CityWithCountry]`で結果の型を指定
2. **ResultSet**: 5つのカラム（city.id、city.name、country.code、country.name、country.region）を持つ結果セット
3. **Decoder構築**: 
   - City用のDecoder: `Decoder[Long]`と`Decoder[String]`を合成して`Decoder[City]`を作成
   - Country用のDecoder: 3つの`Decoder[String]`を合成して`Decoder[Country]`を作成
   - 最終的に両者を合成して`Decoder[CityWithCountry]`を構築
4. **マッピング処理**: 
   - カラム1,2からCityオブジェクトを生成
   - カラム3,4,5からCountryオブジェクトを生成
   - 両者を組み合わせてCityWithCountryインスタンスを生成

このように、ldbcは複雑なネストした構造でも、Decoderの合成により型安全にマッピングを行います。

## タプルを使用した結合クエリ

ネストしたケースクラスの代わりに、タプルを使用して複数テーブルのデータを取得することもできます：

```scala
case class City(id: Long, name: String)
case class Country(code: String, name: String, region: String)

sql"""
  SELECT
    city.id,
    city.name,
    country.code,
    country.name,
    country.region
  FROM city
  JOIN country ON city.country_code = country.code
"""
  .query[(City, Country)]        // Query[(City, Country)]
  .to[List]                      // DBIO[List[(City, Country)]]
  .readOnly(connector)                // IO[List[(City, Country)]]
  .unsafeRunSync()               // List[(City, Country)]
  .foreach { case (city, country) => 
    println(s"City: ${city.name}, Country: ${country.name}")
  }
```

## 単一結果の取得（Option型）

リストではなく、単一の結果や省略可能な結果（0または1件）を取得したい場合は、`.to[Option]`を使用できます：

```scala
case class User(id: Long, name: String, email: String)

// IDによる単一ユーザーの検索
sql"SELECT id, name, email FROM user WHERE id = ${userId}"
  .query[User]                   // Query[User]
  .to[Option]                    // DBIO[Option[User]]
  .readOnly(connector)                // IO[Option[User]]
  .unsafeRunSync()               // Option[User]
  .foreach(user => println(s"Found user: ${user.name}"))
```

結果が見つからない場合は`None`が返され、1件見つかった場合は`Some(User(...))`が返されます。

## クエリ実行メソッドの選択

ldbcでは、用途に応じて異なるクエリ実行メソッドが用意されています：

- `.readOnly(connector)` - 読み取り専用操作に使用します（SELECT文など）
- `.commit(connector)` - 自動コミットモードで書き込み操作を実行します
- `.rollback(connector)` - 書き込み操作を実行し、必ずロールバックします（テスト用）
- `.transaction(connector)` - トランザクション内で操作を実行し、成功時のみコミットします

```scala
// 読み取り専用操作の例
sql"SELECT * FROM users"
  .query[User]
  .to[List]
  .readOnly(connector)

// 書き込み操作の例（自動コミット）
sql"UPDATE users SET name = ${newName} WHERE id = ${userId}"
  .update
  .commit(connector)

// トランザクション内での複数操作
(for {
  userId <- sql"INSERT INTO users (name, email) VALUES (${name}, ${email})".returning[Long]
  _      <- sql"INSERT INTO user_roles (user_id, role_id) VALUES (${userId}, ${roleId})".update
} yield userId).transaction(connector)
```

## コレクション操作とクエリの組み合わせ

取得したデータに対して、Scalaのコレクション操作を適用することで、より複雑なデータ処理を簡潔に記述できます：

```scala
// ユーザーをグループ化する例
sql"SELECT id, name, department FROM employees"
  .query[(Long, String, String)] // ID, 名前, 部署
  .to[List]
  .readOnly(connector)
  .unsafeRunSync()
  .groupBy(_._3) // 部署ごとにグループ化
  .map { case (department, employees) => 
    (department, employees.map(_._2)) // 部署名と従業員名のリストのマッピング
  }
  .foreach { case (department, names) =>
    println(s"Department: $department, Employees: ${names.mkString(", ")}")
  }
```

## ストリーミングによる大量データの効率的な処理

大量のデータを処理する場合、すべてのデータを一度にメモリに読み込むとメモリ不足になる可能性があります。ldbcでは、**ストリーミング**を使ってデータを効率的に処理できます。

### ストリーミングの基本的な使い方

ストリーミングを使用すると、データを少しずつ取得して処理できるため、メモリ使用量を大幅に削減できます：

```scala
import fs2.Stream
import cats.effect.*

// 基本的なストリーミング
val cityStream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream                    // Stream[DBIO, String]

// 最初の5件のみを取得して処理
val firstFiveCities: IO[List[String]] =
  cityStream
    .take(5)                   // 最初の5件のみ
    .compile.toList            // StreamをListに変換
    .readOnly(connector)            // IO[List[String]]
```

### フェッチサイズの指定

`stream(fetchSize: Int)`メソッドを使用して、一度に取得する行数を制御できます：

```scala
// 一度に10行ずつ取得
val efficientStream: Stream[DBIO, String] = 
  sql"SELECT name FROM city"
    .query[String]
    .stream(10)                // fetchSize = 10

// 大量データを効率的に処理
val processLargeData: IO[Int] = 
  sql"SELECT id, name, population FROM city"
    .query[(Long, String, Int)]
    .stream(100)               // 100行ずつ取得
    .filter(_._3 > 1000000)    // 人口100万人以上
    .map(_._2)                 // 都市名のみ取得
    .compile.toList
    .readOnly(connector)
    .map(_.size)
```

### ストリーミングでの実用的なデータ処理

ストリーミングはFs2の`Stream`を返すため、豊富な関数型操作が利用できます：

```scala
// 大量のユーザーデータを段階的に処理
val processUsers: IO[Unit] = 
  sql"SELECT id, name, email, created_at FROM users"
    .query[(Long, String, String, java.time.LocalDateTime)]
    .stream(50)                // 50行ずつ取得
    .filter(_._4.isAfter(lastWeek))  // 先週以降に作成されたユーザー
    .map { case (id, name, email, _) => 
      s"新規ユーザー: $name ($email)"
    }
    .evalMap(IO.println)       // 結果を順次出力
    .compile.drain             // ストリームを実行
    .readOnly(connector)
}
```

### UseCursorFetchによる動作の最適化

MySQLでは`UseCursorFetch`の設定によってストリーミングの効率が大きく変わります：

```scala
// UseCursorFetch=true（推奨）- 真のストリーミング
val efficientDatasource = MySQLDataSource
  .default[IO](host, port, user, password, database)
  .setUseCursorFetch(true)    // サーバーサイドカーソルを有効化
  .setSSL(SSL.None)

// UseCursorFetch=false（デフォルト）- 制限されたストリーミング
val standardDatasource = MySQLDataSource
  .default[IO](host, port, user, password, database)
  .setSSL(SSL.None)
```

**UseCursorFetch=trueの場合：**
- サーバーサイドカーソルを使用して、必要な分だけデータを段階的に取得
- メモリ使用量を大幅に削減（数百万行でも安全）
- 真の意味でのストリーミング処理が可能

**UseCursorFetch=falseの場合：**
- クエリ実行時にすべての結果をメモリに読み込み
- 小さなデータセットでは高速だが、大量データではリスク
- ストリーミングの効果が限定的

### 大量データ処理の実例

以下は100万行のデータを安全に処理する例です：

```scala
// 効率的な大量データ処理
// サーバーサイドカーソルを有効化したDataSourceを作成
val cursorDatasource = MySQLDataSource
  .build[IO](host, port, user)
  .setPassword(password)
  .setDatabase(database)
  .setUseCursorFetch(true)   // 重要：サーバーサイドカーソルを有効化

// Connectorを作成
val cursorConnector = Connector.fromDataSource(cursorDatasource)

val processMillionRecords: IO[Long] = 
  sql"SELECT id, amount FROM transactions WHERE year = 2024"
    .query[(Long, BigDecimal)]
    .stream(1000)          // 1000行ずつ処理
    .filter(_._2 > 100)    // 100円以上の取引のみ
    .map(_._2)             // 金額のみ抽出
    .fold(BigDecimal(0))(_ + _)  // 合計を計算
    .compile.lastOrError   // 最終結果を取得
    .readOnly(cursorConnector)
```

### ストリーミングのメリット

1. **メモリ効率**: 大量データでもメモリ使用量を一定に保てる
2. **早期処理**: データを受信しながら同時に処理できる
3. **中断可能**: 条件に応じて処理を途中で止められる
4. **関数型操作**: `filter`、`map`、`take`などの豊富な操作

```scala
// 条件に応じた早期終了の例
val findFirstLargeCity: IO[Option[String]] = 
  sql"SELECT name, population FROM city ORDER BY population DESC"
    .query[(String, Int)]
    .stream(10)
    .find(_._2 > 5000000)      // 人口500万人以上の最初の都市
    .map(_.map(_._1))          // 都市名のみ取得
    .compile.last
    .readOnly(connector)
```

## マッピングの詳細な仕組み

### Decoderとは

ldbcにおいて、`Decoder`は`ResultSet`からScalaの型への変換を担当する重要なコンポーネントです。Decoderは以下の特徴を持ちます：

1. **型安全性**: コンパイル時に型の整合性を確認
2. **合成可能**: 小さなDecoderを組み合わせて複雑な構造のDecoderを作成
3. **自動導出**: 多くの場合、明示的な定義なしに自動的に生成

### 基本的なDecoderの動作

```scala
// 基本型のDecoder（暗黙的に提供される）
val longDecoder: Decoder[Long] = Decoder.long
val stringDecoder: Decoder[String] = Decoder.string

// 複数のDecoderを合成
val tupleDecoder: Decoder[(Long, String)] = 
  longDecoder *: stringDecoder

// ケースクラスへの変換
case class User(id: Long, name: String)
val userDecoder: Decoder[User] = tupleDecoder.to[User]
```

### カラム番号による読み取り

Decoderは`ResultSet`から値を読み取る際、カラム番号（1から開始）を使用します：

```scala
// decode(columnIndex, resultSet)メソッドの動作
decoder.decode(1, resultSet) // 1番目のカラムを読み取り
decoder.decode(2, resultSet) // 2番目のカラムを読み取り
```

### エラーハンドリング

デコード処理では、以下のようなエラーが発生する可能性があります：

- **型の不一致**: SQLの型とScalaの型が互換性がない
- **NULL値**: NULLを許可しない型へのマッピング
- **カラム数の不一致**: 期待するカラム数と実際のカラム数が異なる

これらのエラーは`Either[Decoder.Error, A]`として表現され、実行時に適切なエラーメッセージが提供されます。

## まとめ

ldbcは、データベースからのデータ取得を型安全かつ直感的に行うための機能を提供しています。このチュートリアルでは、以下の内容を説明しました：

- 基本的なデータ取得のワークフロー
- 単一カラムと複数カラムのクエリ
- ケースクラスへのマッピングと**その内部動作の仕組み**
- **Decoderによる型安全な変換処理**
- 複数テーブルの結合とネストしたデータ構造
- 単一結果と複数結果の取得
- **ストリーミングによる大量データの効率的な処理**
- さまざまな実行メソッド

特に、Decoderの合成によるマッピングの仕組みを理解することで、より複雑なデータ構造でも安全に扱えるようになります。また、ストリーミング機能を活用することで、大量のデータでもメモリ効率的に処理できるようになります。大量データを扱う場合は`UseCursorFetch=true`の設定を検討してください。

## 次のステップ

これでデータベースからさまざまな形式でデータを取得する方法が理解できました。型安全なマッピングにより、データベースの結果を直接Scalaのデータ構造にマッピングできることがわかりました。

次は[データ更新](/ja/tutorial/Updating-Data.md)に進み、データを挿入、更新、削除する方法を学びましょう。
