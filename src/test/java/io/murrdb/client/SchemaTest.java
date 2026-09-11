package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.Nullability;
import io.murrdb.client.table.TableSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaTest extends MurrTest {

    @Test
    void serverReturnsWhatWasCreated() {
        TableSchema schema = TableSchema.builder()
                .key("id")
                .column("z", DType.FLOAT32)
                .column("a", DType.UTF8, Nullability.NOT_NULL)
                .column("m", DType.UINT16)
                .build();
        Table table = createTable("schema", schema);

        TableSchema fetched = join(table.schema());
        assertEquals(schema, fetched);
        assertEquals(List.of("id", "z", "a", "m"), List.copyOf(fetched.columns().keySet()));
        assertFalse(fetched.column("a").nullable());
        assertTrue(fetched.column("m").nullable());

        Map<String, TableSchema> tables = join(client.listTables());
        assertEquals(schema, tables.get(table.name()));
    }

    @Test
    void dropRemovesTable() {
        Table table = createTable("drop", TableSchema.builder().key("id").column("x", DType.INT32).build());
        join(client.dropTable(table.name()));

        assertFalse(join(client.listTables()).containsKey(table.name()));
        assertThrows(TableNotFoundException.class, () -> join(table.schema()));
    }

    @Test
    void dropThenRecreateWithNewSchema() {
        TableSchema before = TableSchema.builder().key("id").column("x", DType.INT32).build();
        Table table = createTable("recreate", before);
        try (Batch batch = Batch.of(before).utf8("id", List.of("k")).int32("x", new int[] {1}).build(allocator)) {
            join(table.write(batch));
        }
        join(table.drop());

        TableSchema after = TableSchema.builder().key("id").column("x", DType.UTF8).build();
        join(client.createTable(table.name(), after));
        assertEquals(after, join(table.schema()));
        // same handle, new table: the old row is gone
        boolean found = join(table.fetch(List.of("k"), List.of("x"), r -> r.found(0)));
        assertFalse(found);
    }

    @Test
    void arrowSchemaRoundTrip() {
        TableSchema.Builder builder = TableSchema.builder().key("id");
        for (DType dtype : DType.values()) {
            builder.column(dtype.name(), dtype, dtype.ordinal() % 2 == 0 ? Nullability.NULLABLE : Nullability.NOT_NULL);
        }
        TableSchema schema = builder.build();
        assertEquals(schema, TableSchema.of("id", schema.toArrowSchema()));
    }
}
