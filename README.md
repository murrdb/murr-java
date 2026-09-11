# murr-java

Java client for [murrdb](https://github.com/murrdb/murr): write batches of rows, fetch a few
columns for a list of keys. Async, HTTP, Arrow underneath. Java 21+.

## Install

```xml
<dependency>
  <groupId>io.murrdb</groupId>
  <artifactId>murr-client</artifactId>
  <version>0.2.2-1</version>
</dependency>
```

Not on Maven Central yet, so `mvn install` from a checkout for now.

Arrow needs one JVM flag, otherwise the client fails on first use:

```
--add-opens=java.base/java.nio=ALL-UNNAMED
```

On JDK 24+ also add `--sun-misc-unsafe-memory-access=allow`.

## Compatibility

The client version is the murrdb version it talks to, plus a client release number after the dash.
murrdb makes no compatibility promises before 1.0, so pin the client to the server version you run.
A client for 0.2.2 may break against 0.2.3.

| murr-client | murrdb |
|-------------|--------|
| 0.2.2-1     | 0.2.2  |
| 0.2.1-1     | 0.2.1  |

## Example

```java
var schema = TableSchema.builder()
    .key("product_id")
    .column("price", DType.FLOAT32, Nullability.NOT_NULL)
    .column("category", DType.UTF8)
    .build();

try (var client = MurrClient.builder().endpoint("http://localhost:8080").build()) {
  Table products = client.createTable("products", schema).join();

  // one column per call, becomes one segment on the server
  try (var batch = Batch.of(schema)
      .utf8("product_id", List.of("p1", "p2", "p3"))
      .float32("price", new float[] {19.99f, 5.5f, 120f})
      .utf8("category", List.of("shoes", "socks", "jackets"))
      .build(client.allocator())) {
    products.write(batch).join();
  }

  // this overload closes the result for you after the lambda returns
  products.fetch(List.of("p1", "p9"), List.of("price"), result -> {
    for (int i = 0; i < result.rowCount(); i++) {
      String key = result.keys().get(i);
      // unknown keys come back as all-null rows
      if (result.found(i)) {
        System.out.println(key + " " + result.float32("price").get(i));
      } else {
        System.out.println(key + " missing");
      }
    }
    return null;
  }).join();
}
```

```
p1 19.99
p9 missing
```

Complete programs, run by the test suite against a real server so they do not rot:

- [`QuickStart.java`](src/test/java/io/murrdb/examples/QuickStart.java): the example above, with a second column and a not-found key.
- [`ArrowRootWrite.java`](src/test/java/io/murrdb/examples/ArrowRootWrite.java): write a `VectorSchemaRoot` you already have, read back the raw Arrow root.
- [`ConcurrentFetch.java`](src/test/java/io/murrdb/examples/ConcurrentFetch.java): twenty fetches in flight on one client, joined with `allOf`.

Scala: `IO.fromCompletableFuture(IO(table.fetch(...)))` works as is.

## Not here

- No embedded mode, you need a running server.
- No Arrow Flight, HTTP only.
- No blocking API. `join()` on a virtual thread is the blocking API.
- No row-to-record mapping yet.

## Development

- Build and test: `mvn verify`. Tests start a murr container, so Docker must be running.
- Run one example: `mvn -q test -Dtest=ExamplesTest#quickStart`.
- Different server image: `MURR_IMAGE=ghcr.io/murrdb/murr:0.3.0 mvn verify`.
- Without Docker: `scripts/start-murr-macos.sh` or `scripts/start-murr-windows.ps1` starts a native server, then `MURR_ENDPOINT=http://127.0.0.1:8080 mvn verify` runs the tests against it.

## License

Apache 2.0
