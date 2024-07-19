# パラメータ化されたクエリ

この章では、パラメータ化されたクエリを構築する方法を学びます。

使用するテーブルは以下の通りです。

```sql
CREATE TABLE country (
  code       character(3)  NOT NULL,
  name       text          NOT NULL,
  population integer       NOT NULL,
  gnp        numeric(10,2)
  -- more columns, but we won't use them here
)
```

```scala
case class Country(code: String, name: String, population: Int, gnp: Option[Double])
```

## パラメータの追加

まずは、パラメーターを持たないクエリを作成します。

```scala
sql"SELECT code, name, population, gnp FROM country".query[Country].to[List]
```

次にクエリをメソッドに組み込んで、ユーザーが指定する国コードと一致するデータのみを選択するパラメーターを追加してみましょう。文字列の補間を行うのと同じように、code引数を$codeとしてSQL文に挿入します。

```scala
val code = "JPN"

sql"SELECT code, name, population, gnp FROM country WHERE code = $code".query[Country].to[List]
```

コネクションを使用してクエリを実行すると問題なく動作します。

```scala
connection.use { conn =>
  sql"SELECT code, name, population, gnp FROM country WHERE code = $code"
    .query[Country]
    .to[List]
    .readOnly(conn)
}
```

ここでは何が起こっているのでしょうか？文字列リテラルをSQL文字列にドロップしているだけのように見えますが、実際にはPreparedStatementを構築しており、code値は最終的にsetStringの呼び出しによって設定されます

## 複数のパラメータ

複数のパラメータも同じように機能する。驚きはない。

```scala
val code = "JPN"
val population = 100000000

connection.use { conn =>
  sql"SELECT code, name, population, gnp FROM country WHERE code = $code AND population > $population"
    .query[Country]
    .to[List]
    .readOnly(conn)
}
```

## IN句の扱い

SQLリテラルを扱う際によくあるイラつきは、一連の引数をIN句にインライン化したいという欲求ですが、SQLはこの概念をサポートしていません（JDBCも何もサポートしていません）。

```scala
val codes = NonEmptyList.of("JPN", "USA", "FRA")

connection.use { conn =>
    sql"SELECT code, name, population, gnp FROM country WHERE" ++ in("code", codes)
        .query[Country]
        .to[List]
        .readOnly(conn)
}
```

IN句は空であってはならないので、コードはNonEmptyListであることに注意。

このクエリーを実行すると、望ましい結果が得られる

ldbcでは他にもいくつかの便利な関数が用意されています。

- `values` - VALUES句を生成する
- `in` - IN句を生成する
- `notIn` - NOT IN句を生成する
- `and` - AND句を生成する
- `or` - OR句を生成する
- `whereAnd` - AND句で括られた複数の条件のWHERE句を生成する
- `whereOr` - OR句で括られた複数の条件のWHERE句を生成する
- `set` - SET句を生成する
- `orderBy` - ORDER BY句を生成する

## 静的なパラメーター

パラメーターは動的ではありますが、時にはパラメーターとして使用しても静的な値として扱いたいことがあるかと思います。

例えば受け取った値に応じて取得するカラムを変更する場合、以下のように記述できます。

```scala
val column = "code"

sql"SELECT $column FROM country".query[String].to[List]
```

動的なパラメーターはPreparedStatementによって処理が行われるため、クエリ文字列自体は`?`で置き換えられます。

そのため、このクエリは`SELECT ? FROM country`として実行されます。

これではログに出力されるクエリがわかりにくいため、`$column`は静的な値として扱いたい場合は、`$column`を`${sc(column)}`とすることで、クエリ文字列に直接埋め込まれるようになります。

```scala
val column = "code"

sql"SELECT ${sc(column)} FROM country".query[String].to[List]
```

このクエリは`SELECT code FROM country`として実行されます。

> `sc(...)`は渡された文字列のエスケープを行わないことに注意してください。ユーザから与えられたデータを渡すことは、インジェクションのリスクになります。
