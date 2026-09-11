package io.murrdb.examples;

import io.murrdb.client.FetchResult;
import io.murrdb.client.MurrClient;
import io.murrdb.client.Table;
import io.murrdb.client.table.TableSchema;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * For code that already has Arrow data, from Parquet, Flight or another library, and does not want
 * to copy it through a {@link io.murrdb.client.Batch}.
 *
 * <p>A {@link VectorSchemaRoot} can be written as is, provided its schema matches the table, key
 * column included. {@code TableSchema.of} derives the table schema from the Arrow one, so the two
 * cannot drift apart. On the read side the plain {@code fetch} hands back a {@link FetchResult} that
 * the caller owns and must close; its {@code root()} can go straight into other Arrow code.
 *
 * <p>{@code mvn -q test -Dtest=ExamplesTest#arrowRootWrite} runs it against a fresh server in Docker.
 * Pass the URL as the first argument to use your own server; there is no drop-table yet, so a second
 * run against the same server fails with {@code TableAlreadyExistsException}. Expected output:
 *
 * <pre>
 * [d2, d1]
 * vec
 * 0.5
 * 0.1
 * </pre>
 */
public final class ArrowRootWrite {

    public static void main(String[] args) {
        String endpoint = args.length > 0 ? args[0] : "http://localhost:8080";

        Schema arrowSchema = new Schema(List.of(
                new Field("id", FieldType.notNullable(ArrowType.Utf8.INSTANCE), null),
                new Field("vec", FieldType.nullable(new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE)), null)));

        try (MurrClient client = MurrClient.builder().endpoint(endpoint).build();
                VectorSchemaRoot root = VectorSchemaRoot.create(arrowSchema, client.allocator())) {
            VarCharVector id = (VarCharVector) root.getVector("id");
            Float4Vector vec = (Float4Vector) root.getVector("vec");
            root.allocateNew();
            id.setSafe(0, "d1".getBytes(StandardCharsets.UTF_8));
            id.setSafe(1, "d2".getBytes(StandardCharsets.UTF_8));
            vec.setSafe(0, 0.1f);
            vec.setSafe(1, 0.5f);
            root.setRowCount(2);

            Table embeddings = client.createTable("embeddings", TableSchema.of("id", arrowSchema)).join();
            embeddings.write(root).join();

            // Rows come back in key order, and only the requested columns are in the root.
            try (FetchResult result = embeddings.fetch(List.of("d2", "d1"), List.of("vec")).join()) {
                System.out.println(result.keys());
                System.out.print(result.root().contentToTSVString());
            }
        }
    }
}
