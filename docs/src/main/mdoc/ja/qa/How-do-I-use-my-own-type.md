{%
  laika.title = "Q: 独自の型を使うにはどうすればいいですか？"
  laika.metadata.language = ja
%}

# Q: 独自の型を使うにはどうすればいいですか？

## A: 独自の型を利用するには、まずその型に対するEncoderとDecoderを定義します。これにより、データベースに値をバインドしたり、クエリ結果から値を取り出す際に、カスタム型として扱うことが可能になります。例えば、ユーザーの状態を表すStatusという型を次のように定義できます。

```scala 3
// 独自の型Statusの定義
enum Status(val active: Boolean, val label: String):
  case Active   extends Status(true, "Active")
  case Inactive extends Status(false, "Inactive")

// Encoderを定義してStatusをBooleanとしてバインドする
given Encoder[Status] = Encoder[Boolean].contramap(_.active)

// Decoderを定義してBooleanからStatusに変換する
given Decoder[Status] = Decoder[Boolean].map {
  case true  => Status.Active
  case false => Status.Inactive
}

// Codecを使ってEncoderとDecoderを統合する例
given Codec[Status] =
  Codec[Boolean].imap(b => if b then Status.Active else Status.Inactive)(_.active)
```

上記のサンプルでは、Status型は実際にはBoolean値に変換され、データベースへのINSERTやクエリ結果のデコードで利用されます。これにより、データベースとのやりとりにおいて型安全が保たれ、カスタムロジックを簡単に統合できます。

また、複数の型を合成して新しい型を作成する場合は、次のようにEncoderやDecoderを合成できます。

```scala 3
// 2つの値を組み合わせてタプルからカスタム型に変換する例
case class CustomStatus(code: Int, label: String)
given Encoder[CustomStatus] = (Encoder[Int] *: Encoder[String]).to[CustomStatus]
given Decoder[CustomStatus] = (Decoder[Int] *: Decoder[String]).to[CustomStatus]
// or
given Codec[CustomStatus] = (Codec[Int] *: Codec[String]).to[CustomStatus]
```

このように、独自の型に対するEncoder、Decoder、Codecを定義することで、ldbcを通じたデータ操作において、カスタム型が自然に扱えるようになります。

## 参考資料
- [カスタム データ型](/ja/tutorial/Custom-Data-Type.md)
- [Codecの使い方](/ja/tutorial/Custom-Data-Type.md#codec)
