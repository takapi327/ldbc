{%
  laika.title = "Q: How to use connection pool with Scala connector?"
  laika.metadata.language = en
%}

# Q: How to use connection pool with Scala connector?

## A: Currently, the Scala connector does not support connection pooling.

The Scala connector does not yet support connection pooling. If you want to use a connection pool, please use the Java connector with connection pool libraries like [HikariCP](https://github.com/brettwooldridge/HikariCP).

## References
- [HikariCP](/en/examples/HikariCP.md)
- [Using JDBC connector](/en/tutorial/Connection.md#using-the-jdbc-connector)
