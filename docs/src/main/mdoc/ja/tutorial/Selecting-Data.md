{%
  laika.title = データ選択
  laika.metadata.language = ja
%}

# データ選択

[パラメータ化クエリ](/ja/tutorial/Parameterized-Queries.md)の使い方を学んだところで、今度はさまざまな形式でデータを取得する方法を見ていきましょう。このページでは、SELECTクエリを使ってデータを効率的に取得し、Scalaの型にマッピングする方法を説明します。

ldbcの最も強力な機能の1つは、データベースの結果をScalaの型に簡単にマッピングできることです。単純なプリミティブ型から複雑なケースクラスまで、さまざまなデータ形式を扱えます。

## データ取得の基本ワークフロー

ldbcでデータを取得する基本的な流れは以下の通りです：

1. SQLクエリを`sql`補間子で作成
2. `.query[T]`で結果の型を指定
3. `.to[Collection]`で結果をコレクションに変換（オプション）
4. `.readOnly()`/`.commit()`/`.transaction()`などでクエリを実行
5. 結果を処理

この流れをコード上の型の変化と共に見ていきましょう。

## コレクションへの行の読み込み

最初のクエリでは、いくつかのユーザー名をリストに取得し、出力する例を見てみましょう。各ステップで型がどう変化するかを示しています：

```scala
sql"SELECT name FROM user"
  .query[String]                 // Query[String]
  .to[List]                      // DBIO[List[String]]
  .readOnly(conn)                // IO[List[String]]
  .unsafeRunSync()               // List[String]
  .foreach(println)              // Unit
```

このコードを詳しく説明すると：

- `sql"SELECT name FROM user"` - SQLクエリを定義します。
- `.query[String]` - 各行の結果を`String`型にマッピングします。これにより`Query[String]`型が生成されます。
- `.to[List]` - 結果を`List`に集約します。`DBIO[List[String]]`型が生成されます。このメソッドは`FactoryCompat`を実装する任意のコレクション型（`List`、`Vector`、`Set`など）で使用できます。
- `.readOnly(conn)` - コネクションを読み取り専用モードで使用してクエリを実行します。戻り値は`IO[List[String]]`です。
- `.unsafeRunSync()` - IOモナドを実行して実際の結果（`List[String]`）を取得します。
- `.foreach(println)` - 結果の各要素を出力します。

## 複数列クエリ

もちろん、複数のカラムを選択してタプルにマッピングすることもできます：

```scala
sql"SELECT name, email FROM user"
  .query[(String, String)]       // Query[(String, String)]
  .to[List]                      // DBIO[List[(String, String)]]
  .readOnly(conn)                // IO[List[(String, String)]]
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
  .readOnly(conn)                // IO[List[User]]
  .unsafeRunSync()               // List[User]
  .foreach(user => println(s"ID: ${user.id}, Name: ${user.name}, Email: ${user.email}"))
```

**重要**: ケースクラスのフィールド名とSQLクエリで選択するカラム名が一致している必要があります。順序も一致している必要がありますが、名前が正確に一致していれば、ldbcが適切にマッピングします。

![Selecting Data](../../img/data_select.png)

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
  .readOnly(conn)                // IO[List[CityWithCountry]]
  .unsafeRunSync()               // List[CityWithCountry]
  .foreach(cityWithCountry => println(
    s"City: ${cityWithCountry.city.name}, Country: ${cityWithCountry.country.name}"
  ))
```

ldbcの特徴として、`テーブル名.カラム名`の形式で指定されたカラムは、自動的に`クラス名.フィールド名`にマッピングされます。これにより、上記の例では次のようなマッピングが行われます：

- `city.id` → `CityWithCountry.city.id`
- `city.name` → `CityWithCountry.city.name`
- `country.code` → `CityWithCountry.country.code`
- `country.name` → `CityWithCountry.country.name`
- `country.region` → `CityWithCountry.country.region`

![Selecting Data](../../img/data_multi_select.png)

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
  .readOnly(conn)                // IO[List[(City, Country)]]
  .unsafeRunSync()               // List[(City, Country)]
  .foreach { case (city, country) => 
    println(s"City: ${city.name}, Country: ${country.name}")
  }
```

ここで重要なのは、タプルを使用する場合、テーブル名とケースクラスの名前は一致している必要があるということです。つまり、`city`テーブルは`City`クラスに、`country`テーブルは`Country`クラスにマッピングされます。

## テーブルのエイリアスとマッピング

SQL文でテーブルにエイリアスを使用する場合、ケースクラスの名前もそのエイリアスと一致させる必要があります：

```scala
// エイリアス名に合わせたケースクラス名
case class C(id: Long, name: String)
case class CT(code: String, name: String, region: String)

sql"""
  SELECT
    c.id,
    c.name,
    ct.code,
    ct.name,
    ct.region
  FROM city AS c
  JOIN country AS ct ON c.country_code = ct.code
"""
  .query[(C, CT)]                // Query[(C, CT)]
  .to[List]                      // DBIO[List[(C, CT)]]
  .readOnly(conn)                // IO[List[(C, CT)]]
  .unsafeRunSync()               // List[(C, CT)]
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
  .readOnly(conn)                // IO[Option[User]]
  .unsafeRunSync()               // Option[User]
  .foreach(user => println(s"Found user: ${user.name}"))
```

結果が見つからない場合は`None`が返され、1件見つかった場合は`Some(User(...))`が返されます。

## クエリ実行メソッドの選択

ldbcでは、用途に応じて異なるクエリ実行メソッドが用意されています：

- `.readOnly(conn)` - 読み取り専用操作に使用します（SELECT文など）
- `.commit(conn)` - 自動コミットモードで書き込み操作を実行します
- `.rollback(conn)` - 書き込み操作を実行し、必ずロールバックします（テスト用）
- `.transaction(conn)` - トランザクション内で操作を実行し、成功時のみコミットします

```scala
// 読み取り専用操作の例
sql"SELECT * FROM users"
  .query[User]
  .to[List]
  .readOnly(conn)

// 書き込み操作の例（自動コミット）
sql"UPDATE users SET name = ${newName} WHERE id = ${userId}"
  .update
  .commit(conn)

// トランザクション内での複数操作
(for {
  userId <- sql"INSERT INTO users (name, email) VALUES (${name}, ${email})".returning[Long]
  _      <- sql"INSERT INTO user_roles (user_id, role_id) VALUES (${userId}, ${roleId})".update
} yield userId).transaction(conn)
```

## コレクション操作とクエリの組み合わせ

取得したデータに対して、Scalaのコレクション操作を適用することで、より複雑なデータ処理を簡潔に記述できます：

```scala
// ユーザーをグループ化する例
sql"SELECT id, name, department FROM employees"
  .query[(Long, String, String)] // ID, 名前, 部署
  .to[List]
  .readOnly(conn)
  .unsafeRunSync()
  .groupBy(_._3) // 部署ごとにグループ化
  .map { case (department, employees) => 
    (department, employees.map(_._2)) // 部署名と従業員名のリストのマッピング
  }
  .foreach { case (department, names) =>
    println(s"Department: $department, Employees: ${names.mkString(", ")}")
  }
```

## まとめ

ldbcは、データベースからのデータ取得を型安全かつ直感的に行うための機能を提供しています。このチュートリアルでは、以下の内容を説明しました：

- 基本的なデータ取得のワークフロー
- 単一カラムと複数カラムのクエリ
- ケースクラスへのマッピング
- 複数テーブルの結合とネストしたデータ構造
- 単一結果と複数結果の取得
- さまざまな実行メソッド

これらの知識を活用して、アプリケーション内でデータベースから効率的にデータを取得し、Scalaの型システムの利点を最大限に活用してください。

## 次のステップ

これでデータベースからさまざまな形式でデータを取得する方法が理解できました。型安全なマッピングにより、データベースの結果を直接Scalaのデータ構造にマッピングできることがわかりました。

次は[データ更新](/ja/tutorial/Updating-Data.md)に進み、データを挿入、更新、削除する方法を学びましょう。
