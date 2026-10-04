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
 * <p>A {@link VectorSchemaRoot} can be written as is, provided it holds every column of the table,
 * key columns included. {@code TableSchema.of} derives the table schema from the Arrow one, so the two
 * cannot drift apart. On the read side the keys are a root as well, holding only the key columns, and
 * the plain {@code fetch} hands back a {@link FetchResult} that the caller owns and must close; its
 * {@code root()} can go straight into other Arrow code.
 *
 * <p>{@code mvn -q test -Dtest=ExamplesTest#arrowRootWrite} runs it against a fresh server in Docker.
 * Pass the URL as the first argument to use your own server; the table is dropped at the end, so it
 * can run again against the same server. Expected output:
 *
 * <pre>
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

            Table embeddings = client.createTable("embeddings", TableSchema.of(arrowSchema, "id")).join();
            embeddings.write(root).join();

            // Keys travel as Arrow too: a root holding just the key column, here d2 then d1.
            try (VectorSchemaRoot keys = VectorSchemaRoot.create(new Schema(List.of(arrowSchema.findField("id"))), client.allocator())) {
                VarCharVector wanted = (VarCharVector) keys.getVector("id");
                keys.allocateNew();
                wanted.setSafe(0, "d2".getBytes(StandardCharsets.UTF_8));
                wanted.setSafe(1, "d1".getBytes(StandardCharsets.UTF_8));
                keys.setRowCount(2);

                // Rows come back in key order, and only the requested columns are in the root.
                try (FetchResult result = embeddings.fetch(keys, List.of("vec")).join()) {
                    System.out.print(result.root().contentToTSVString());
                }
            }

            embeddings.drop().join();
        }
    }
}
