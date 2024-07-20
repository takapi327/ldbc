{%
laika.title = ldbc
laika.metadata.language = ja
%}

# ldbc (Lepus Database Connectivity)

**ldbc**は1.0以前のソフトウェアであり、現在も活発に開発中であることに注意してください。新しいバージョンは以前のバージョンとバイナリ互換性がなくなってしまう可能性があります。

ldbcは、[Cats Effect 3](https://typelevel.org/cats-effect/)と[Scala 3](https://github.com/scala/scala3)による純粋関数型JDBCレイヤーを構築するためのライブラリです。

ldbcは[Typelevel](http://typelevel.org/)プロジェクトです。これは、Scalaの[行動規範](http://scala-lang.org/conduct.html)に記載されているように、純粋で、型にはまらない、関数型プログラミングを受け入れ、教育、学習、貢献のための安全でフレンドリーな環境を提供することを意味します。

## はじめに

私たちのアプリケーション開発では大抵の場合データベースを使用します。<br>Scalaでデータベースアクセスを行う場合JDBCを使用する方法がありますが、ScalaにはこのJDBCをラップしたライブラリがいくつか存在しています。

- 関数型DSL (Slick, quill, zio-sql)
- SQL文字列インターポレーター (Anorm, doobie)

ldbcも同じくJDBCをラップしたライブラリであり、ldbcはそれぞれの側面を組み合わせたScala 3ライブラリで、型安全でリファクタブルなSQLインターフェイスを提供し、MySQLのデータベース上でのSQL式を表現できます。

ldbcは他のライブラリとは異なり、Scalaで構築された独自のコネクターも提供しています。

Scalaは現在JVM, JS, Nativeというマルチプラットフォームに対応しています。

しかし、JDBCを使用したライブラリだとJVM環境でしか動作しません。<br>
そのためldbcは、MySQLプロトコルに対応したScalaで書かれたコネクタを提供することで、異なるプラットフォームで動作できるようにするために開発を行っています。<br>
ldbcを使用することで、Scalaの型安全性と関数型プログラミングの利点を活かしながら、プラットフォームを問わずにデータベースアクセスを行うことができます。

また、ldbcを使用することで単一リソースを管理することでScalaのモデルやsqlのスキーマ、ドキュメントを一元化できる開発を行えることです。

このコンセプトは宣言的でタイプセーフなWebエンドポイントライブラリである[tapir](https://github.com/softwaremill/tapir)から影響を受けました。<br>tapirを使用することで型安全なエンドポイントを構築することができ、構築したエンドポイントからOpenAPIドキュメントを生成することもできます。

ldbcはデータベース層でScalaを使用して、同じように型安全な構築を可能にし、構築されたものを使用してドキュメントの生成を行えるようにします。

### 対象読者

このドキュメントは、Scalaプログラミング言語を使用してデータベースアクセスを行うためのライブラリであるldbcを使用する開発者を対象としています。

ldbcは、型付けされた純粋な関数型プログラミングに興味がある人のために設計されています。もしあなたがCatsユーザーでなかったり、関数型I/OやモナドCats Effectに馴染みがなかったりする場合は、ゆっくり進める必要があるかもしれません。

とはいえ、もしこのドキュメントやldbc APIに戸惑ったり苛立ったりしたら、issueを発行して助けを求めてください。ライブラリもドキュメントも歴史が浅く、急速に変化しているため、不明瞭な点があるのは避けられません。従って、本書は問題や脱落に対処するために継続的に更新されます。

## クイックスタート

現在のバージョンは **Scala @SCALA_VERSION@** に対応した **@VERSION@** です。

```scala
libraryDependencies ++= Seq(

  // まずはこの1つから
  "@ORGANIZATION@" %% "ldbc-dsl" % "@VERSION@",
  
  // 使用するコネクタを選択
  "@ORGANIZATION@" %% "jdbc-connector" % "@VERSION@", // Javaコネクタ (対応プラットフォーム: JVM)
  "@ORGANIZATION@" %% "ldbc-connector" % "@VERSION@", // Scalaコネクタ (対応プラットフォーム: JVM, JS, Native)

  // そして、必要に応じてこれらを加える
  "@ORGANIZATION@" %% "ldbc-query-builder" % "@VERSION@", // 型安全なクエリ構築
  "@ORGANIZATION@" %% "ldbc-schema"        % "@VERSION@", // データベーススキーマの構築
)
```

## TODO

- JSONデータタイプのサポート
- SETデータタイプのサポート
- Geometryデータタイプのサポート
- CHECK制約のサポート
- MySQL以外のデータベースサポート
- ストリーミングのサポート
- ZIOモジュールのサポート
- テストキット
- etc...
