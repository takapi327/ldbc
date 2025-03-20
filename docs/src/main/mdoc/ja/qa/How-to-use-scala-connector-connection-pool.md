{%
  laika.title = "Q: Scalaコネクタでコネクションプールを使用する方法は？"
  laika.metadata.language = ja
%}

# Q: Scalaコネクタでコネクションプールを使用する方法は？

## A: 現在、Scalaコネクタはコネクションプールをサポートしていません。

Scalaコネクタではまだコネクションプールをサポートできていません。コネクションプールを使用したい場合は、Javaのコネクタを使用して[HikariCP](https://github.com/brettwooldridge/HikariCP)などのコネクションプールライブラリを使用してください。

## 参考資料
- [HikariCP](/ja/examples/HikariCP.md)
- [jdbcコネクタの使用](/ja/tutorial/Connection.md#jdbcコネクタの使用)
[Implicit-search-problem-too-large.md](Implicit-search-problem-too-large.md)