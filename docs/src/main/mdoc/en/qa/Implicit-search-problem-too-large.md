{%
  laika.title = "Q: How to handle the `Implicit search problem too large.` error?"
  laika.metadata.language = en
%}

# Q: How to handle the `Implicit search problem too large.` error?

```shell
[error]    |Implicit search problem too large.
[error]    |an implicit search was terminated with failure after trying 100000 expressions.
[error]    |The root candidate for the search was:
[error]    |
[error]    |  given instance given_Decoder_P in object Decoder  for  ldbc.dsl.codec.Decoder[City]}
```

# A: When `Implicit search problem too large.` occurs, you can solve it by increasing the recursion depth using the `-Ximplicit-search-limit` option.

When the `Implicit search problem too large.` error occurs, you can often solve the problem by increasing the search limit compilation option. You can set any value for the number indicating the recursion depth.

```scala 3
scalacOptions += "-Ximplicit-search-limit:100000"
```

However, expanding the limit through this option may lead to increased compilation time. In that case, you can also solve it by manually constructing the required type as shown below:

```scala 3
given Decoder[City] = Decoder.derived[City]
// Or given Decoder[City] = (Decoder[Int] *: Decoder[String] *: Decoder[Int] *: ....).to[City]
given Encoder[City] = Encoder.derived[City]
// Or given Encoder[City] = (Encoder[Int] *: Encoder[String] *: Encoder[Int] *: ....).to[City]
```

Alternatively, you can solve it by using `Codec` to combine `Encoder` and `Decoder`.

```scala 3
given Codec[City] = Codec.derived[City]
// Or given Codec[City] = (Codec[Int] *: Codec[String] *: Codec[Int] *: ....).to[City]
```

## References
- [Custom Data Type](/en/tutorial/Custom-Data-Type.md)
