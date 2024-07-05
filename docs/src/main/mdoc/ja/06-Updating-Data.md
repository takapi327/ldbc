# データ更新

この章では、データベースのデータを変更する操作と、更新結果を取得する方法について説明します。

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

## 挿入

挿入は簡単で、selectと同様に動作します。ここでは、`task`テーブルに行を挿入するExecutorを作成するメソッドを定義します。

```scala
def insertTask(name: String, done: Boolean): Executor[IO, Int] =
  sql"INSERT INTO task (name, done) VALUES ($name, $done)"
    .update // Executor[IO, Int]
```

いくつかの行を挿入してみよう。

```scala
insertTask("task1", done = false).commit.unsafeRunSync()
insertTask("task2", done = true).commit.unsafeRunSync()
insertTask("task3", done = false).commit.unsafeRunSync()
```

そして読み返す。

```scala
sql"SELECT * FROM task"
  .query[(Int, String, Boolean)] // Query[IO, (Int, String, Boolean)]
  .to[List] // Executor[IO, List[(Int, String, Boolean)]]
  .readOnly(conn) // IO[List[(Int, String, Boolean)]]
  .unsafeRunSync() // List[(Int, String, Boolean)]
  .foreach(println) // Unit
// (1,task1,false)
// (2,task2,true)
// (3,task3,false)
```

## 更新

更新も同じパターンだ。ここではタスクを完了済みに更新する。

```scala
def updateTaskDone(id: Int): Executor[IO, Int] =
  sql"UPDATE task SET done = ${true} WHERE id = $id"
    .update // Executor[IO, Int]
```

結果の取得

```scala
updateTaskDone(1).commit.unsafeRunSync()

sql"SELECT * FROM task WHERE id = 1"
  .query[(Int, String, Boolean)] // Query[IO, (Int, String, Boolean)]
  .to[Option] // Executor[IO, List[(Int, String, Boolean)]]
  .readOnly(conn) // IO[List[(Int, String, Boolean)]]
  .unsafeRunSync() // List[(Int, String, Boolean)]
  .foreach(println) // Unit
// Some((1,task1,true))
```

## 自動生成キー

インサートする際には、新しく生成されたキーを返したいものです。まず、挿入して最後に生成されたキーを`LAST_INSERT_ID`で取得し、指定された行を選択するという難しい方法をとります。

```scala
def insertTask(name: String, done: Boolean): Executor[IO, (Int, String, Boolean)] =
  for {
    _ <- sql"INSERT INTO task (name, done) VALUES ($name, $done)".update
    id <- sql"SELECT LAST_INSERT_ID()".query[Int].unsafe
    task <- sql"SELECT * FROM task WHERE id = $id".query[(Int, String, Boolean)].to[Option]
  } yield task
```

```scala
insertTask("task4", done = false).commit.unsafeRunSync()
```

これは苛立たしいことだが、すべてのデータベースでサポートされている（ただし、「最後に使用されたIDを取得する」機能はベンダーによって異なる）。

MySQLでは、`AUTO_INCREMENT`が設定された行のみが挿入時に返すことができます。上記の操作を2つのステートメントに減らすことができます

自動生成キーを使用して行を挿入する場合、`returning`メソッドを使用して自動生成キーを取得できます。

```scala
def insertTask(name: String, done: Boolean): Executor[IO, (Int, String, Boolean)] =
  for {
    id <- sql"INSERT INTO task (name, done) VALUES ($name, $done)".returning[Long]
    task <- sql"SELECT * FROM task WHERE id = $id".query[(Int, String, Boolean)].to[Option]
  } yield task
```

```scala
insertTask("task5", done = false).commit.unsafeRunSync()
```

## バッチ更新

バッチ更新を行うには、`NonEmptyList`を使用して複数の行を挿入する`insertManyTask`メソッドを定義します。

```scala
def insertManyTask(tasks: NonEmptyList[(String, Boolean)]): Executor[IO, Int] = {
  val value = tasks.map { case (name, done) => sql"($name, $done)" }
  (sql"INSERT INTO task (name, done) VALUES" ++ values(value)).update
}
```

このプログラムを実行すると、更新された行数が得られる。

```scala
insertManyTask(NonEmptyList.of(("task6", false), ("task7", true), ("task8", false))).commit.unsafeRunSync()
```
