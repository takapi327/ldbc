{%
  laika.title = パラメータ
  laika.metadata.language = ja
%}

# パラメータ化されたクエリ

この章では、パラメータ化されたクエリを構築する方法を学びます。

## パラメータの追加

まずは、パラメーターを持たないクエリを作成します。

```scala
sql"SELECT name, email FROM user".query[(String, String)].to[List]
```

次にクエリをメソッドに組み込んで、ユーザーが指定する`id`と一致するデータのみを選択するパラメーターを追加してみましょう。文字列の補間を行うのと同じように、`id`引数を`$id`としてSQL文に挿入します。

```scala
val id = 1

sql"SELECT name, email FROM user WHERE id = $id".query[(String, String)].to[List]
```

コネクションを使用してクエリを実行すると問題なく動作します。

```scala
connection.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

ここでは何が起こっているのでしょうか？文字列リテラルをSQL文字列にドロップしているだけのように見えますが、実際には`PreparedStatement`を構築しており、`id`値は最終的に`setInt`の呼び出しによって設定されます。

## 複数のパラメータ

複数のパラメータも同じように機能する。驚きはない。

```scala
val id = 1
val email = "alice@example.com"

connection.use { conn =>
  sql"SELECT name, email FROM user WHERE id = $id AND email > $email"
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

## IN句の扱い

SQLリテラルを扱う際によくあるイラつきは、一連の引数をIN句にインライン化したいという欲求ですが、SQLはこの概念をサポートしていません（JDBCも何もサポートしていません）。

```scala
val ids = NonEmptyList.of(1, 2, 3)

connection.use { conn =>
  sql"SELECT name, email FROM user WHERE" ++ in("id", ids)
    .query[(String, String)]
    .to[List]
    .readOnly(conn)
}
```

IN句は空であってはならないので、`ids`は`NonEmptyList`であることに注意。

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
val column = "name"

sql"SELECT $column FROM user".query[String].to[List]
```

動的なパラメーターは`PreparedStatement`によって処理が行われるため、クエリ文字列自体は`?`で置き換えられます。

そのため、このクエリは`SELECT ? FROM user`として実行されます。

これではログに出力されるクエリがわかりにくいため、`$column`は静的な値として扱いたい場合は、`$column`を`${sc(column)}`とすることで、クエリ文字列に直接埋め込まれるようになります。

```scala
val column = "name"

sql"SELECT ${sc(column)} FROM user".query[String].to[List]
```

このクエリは`SELECT name FROM user`として実行されます。

> `sc(...)`は渡された文字列のエスケープを行わないことに注意してください。ユーザから与えられたデータを渡すことは、インジェクションのリスクになります。
