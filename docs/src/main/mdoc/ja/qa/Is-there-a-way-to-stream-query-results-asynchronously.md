{%
laika.title = "Q: クエリの結果を非同期でストリーミングする方法はありますか？"
laika.metadata.language = ja
%}

# Q: クエリの結果を非同期でストリーミングする方法はありますか？

## A: ストリーミングAPIをサポートしています。

ストリーミング処理は`stream`メソッドを使用して行うことができます。これによって戻り値はFs2の`Stream`になります。

```scala 3
val stream: fs2.Stream[DBIO, String] = sql"SELECT * FROM `table`".query[String].stream
```

MySQLでは`UseCursorFetch`の設定によってストリーミングの動作が大きく変わります：

- **UseCursorFetch=true**: サーバーサイドカーソルを使用して真のストリーミング処理
- **UseCursorFetch=false**: 制限されたストリーミング処理（メモリ制約あり）

大量データを扱う場合は`UseCursorFetch=true`の設定を推奨します。

## 参考資料
- [データ選択 - ストリーミングによる大量データの効率的な処理](/ja/tutorial/Selecting-Data.md#ストリーミングによる大量データの効率的な処理)
