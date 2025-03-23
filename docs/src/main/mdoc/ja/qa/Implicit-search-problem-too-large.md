{%
  laika.title = "Q: `Implicit search problem too large.`が発生する場合の対処方法は？"
  laika.metadata.language = ja
%}

# Q: `Implicit search problem too large.`が発生する場合の対処方法は？

```shell
[error]    |Implicit search problem too large.
[error]    |an implicit search was terminated with failure after trying 100000 expressions.
[error]    |The root candidate for the search was:
[error]    |
[error]    |  given instance given_Decoder_P in object Decoder  for  ldbc.dsl.codec.Decoder[City]}
```

# A: `Implicit search problem too large.`が発生する場合、`-Ximplicit-search-limit`オプションを使用して再帰の深さを増やすことで解決できます。

`Implicit search problem too large.`エラーが発生する場合は、コンパイルオプションの検索制限を上げると問題が解決することがあります。再帰の深さを示す数字は任意の値を設定してください。

```scala 3
scalacOptions += "-Ximplicit-search-limit:100000"
```

しかし、オプションでの制限拡張はコンパイル時間の増幅につながる可能性があります。その場合は、以下のように手動で任意の型を構築することで解決することもできます。

```scala 3
given Decoder[City] = Decoder.derived[City]
// Or given Decoder[City] = (Decoder[Int] *: Decoder[String] *: Decoder[Int] *: ....).to[City]
given Encoder[City] = Encoder.derived[City]
// Or given Encoder[City] = (Encoder[Int] *: Encoder[String] *: Encoder[Int] *: ....).to[City]
```

もしくは、`Codec`を使用して`Encoder`と`Decoder`を組み合わせることで解決することもできます。

```scala 3
given Codec[City] = Codec.derived[City]
// Or given Codec[City] = (Codec[Int] *: Codec[String] *: Codec[Int] *: ....).to[City]
```

## 参考資料
- [カスタム データ型](/ja/tutorial/Custom-Data-Type.md)
