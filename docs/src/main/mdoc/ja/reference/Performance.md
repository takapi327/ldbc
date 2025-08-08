{%
laika.title = パフォーマンス
laika.metadata.language = ja
%}

# パフォーマンス

現在サポートされているJavaのバージョンは11、17、21〜です。それぞれのバージョンで、ldbcとjdbcのパフォーマンスを比較しました。

| Operation | 11 | 17 | 21~ |
|-----------|:--:|:--:|:---:|
| Reading   | 🔺 | 🔺 |  ✅  |
| Writing   | ✅  | ✅  |  ✅  |

- 🔺: パフォーマンスに改善の余地あり
- ✅: パフォーマンスが安定している

パフォーマンス結果から分かる通り、最新のバージョンであるJDK 21~が最も高いパフォーマンスを示しています。特に、ldbcのprepareStatementとstatementは、高いスループットを維持しており、高負荷時にも安定しています。ldbcを使用する場合は、JDK 21以上のバージョンを使用することをお勧めします。

## クエリ実行のオーバーヘッド

### 書き込み

ベンチマークは、異なる操作に対する1秒あたりの操作数（ops/s）を測定しました。以下のグラフは、JDKのバージョンごとにldbcとjdbcのパフォーマンスを示しています。

**JDK 11**

ldbcとjdbcのパフォーマンスはほぼ同等であり、特定の操作においてはldbcが若干優れていることが確認されました。

@:image(/img/connector/Insert.svg) {
alt = "Select Benchmark (JDK 11)"
}

**JDK 17**

JDK 11と比較して、ldbcのパフォーマンスが向上していることがわかります。特に、複雑なクエリに対する応答時間が短縮されています。

jdbcも同様にパフォーマンスが向上していますが、ldbcの方が一貫して高いパフォーマンスを示しています。

@:image(/img/connector/Insert17.svg) {
alt = "Select Benchmark (JDK 17)"
}

**JDK 21**

ldbcのパフォーマンスがさらに向上し、jdbcを上回る結果が多く見られます。

特に、大量のデータを扱う操作において、ldbcの優位性が顕著です。

@:image(/img/connector/Insert21.svg) {
alt = "Select Benchmark (JDK 21)"
}

ベンチマーク結果から、ldbcはJDKのバージョンが上がるにつれてパフォーマンスが向上しており、jdbcと比較して一貫して高いパフォーマンスを示しています。特に、JDK 21を使用する場合、ldbcは大規模なデータ操作において優れたパフォーマンスを発揮します。

これらの結果を考慮すると、ldbcを使用することでパフォーマンスの懸念を払拭できる可能性が高いです。特に、最新のJDKを使用することで、ldbcの利点を最大限に活用できるでしょう。

### 読み取り

JDK 21を使用した ベンチマーク結果から、ldbcとjdbcのパフォーマンスを比較しました。以下のグラフは、異なる操作に対する1秒あたりの操作数（ops/s）を示しています。

- ldbc: prepareStatement - 青色の線
- ldbc: statement - オレンジ色の線
- jdbc: prepareStatement - 緑色の線
- jdbc: statement - 赤色の線

すべてのバージョンの中で最も高いスループットを示しています。`ldbc: prepareStatement`および`jdbc: statement`の両方で優れたパフォーマンスを発揮しており、最新の最適化が効果を発揮していることがわかります。

@:image(/img/connector/Select21.svg) {
alt = "Select Benchmark (JDK 21)"
}

#### パフォーマンスの比較

**1. ldbc: prepareStatement vs jdbc: prepareStatement**

ldbcのprepareStatementは、jdbcのprepareStatementと比較して一貫して高いパフォーマンスを示しています。特に高負荷時においても、ldbcは安定した操作数を維持しています。

**2. ldbc: statement vs jdbc: statement**

ldbcのstatementも、jdbcのstatementと比較して優れたパフォーマンスを発揮しています。ldbcは、特に複雑なクエリや大量のデータ処理において、jdbcよりも効率的に動作します。

ベンチマーク結果から、ldbcはjdbcと比較して以下の点で優れたパフォーマンスを示しています

- 高いスループット：ldbcは、1秒あたりの操作数がjdbcよりも高く、特に高負荷時においても安定しています。
- 効率的なリソース使用：ldbcは、複雑なクエリや大量のデータ処理においても効率的に動作し、リソースの使用を最適化します。

これらの結果から、ldbcを使用することで、パフォーマンスの懸念を払拭できると判断できます。ldbcは、特に高負荷環境や大量のデータ処理が必要なシナリオにおいて、優れた選択肢となるでしょう。

#### その他のバージョン

その他のバージョンにおけるベンチマーク結果を以下に示します。

**JDK 11**

このバージョンでは、スループットが他のバージョンと比較して低い傾向があります。特に、`ldbc: prepareStatement`のパフォーマンスが他のバージョンに比べて劣っています。

@:image(/img/connector/Select.svg) {
alt = "Select Benchmark (JDK 11)"
}

**JDK 17**

JDK 11と比較して、全体的なスループットが向上しています。`jdbc: prepareStatement`のパフォーマンスが特に改善されており、安定した結果を示しています。

@:image(/img/connector/Select17.svg) {
alt = "Select Benchmark (JDK 17)"
}
