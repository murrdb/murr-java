package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.table.ColumnSchema;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SchemaTest extends MurrTest {

    @Test
    void serverReturnsWhatWasCreated() {
        TableSchema schema = TableSchema.builder()
                .key("id", DType.UTF8)
                .column("z", DType.FLOAT32)
                .column("a", ColumnSchema.of(DType.UTF8).nullable(false))
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
        Table table = createTable("drop", TableSchema.builder().key("id", DType.UTF8).column("x", DType.INT32).build());
        join(client.dropTable(table.name()));

        assertFalse(join(client.listTables()).containsKey(table.name()));
        assertThrows(TableNotFoundException.class, () -> join(table.schema()));
    }

    @Test
    void dropThenRecreateWithNewSchema() {
        TableSchema before = TableSchema.builder().key("id", DType.UTF8).column("x", DType.INT32).build();
        Table table = createTable("recreate", before);
        try (Batch batch = Batch.of(before).utf8("id", List.of("k")).int32("x", new int[] {1}).build(allocator)) {
            join(table.write(batch));
        }
        join(table.drop());

        TableSchema after = TableSchema.builder().key("id", DType.UTF8).column("x", DType.UTF8).build();
        join(client.createTable(table.name(), after));
        assertEquals(after, join(table.schema()));
        // same handle, new table: the old row is gone
        boolean found = join(table.fetch(byId(List.of("k"), List.of("x")), r -> r.found(0)));
        assertFalse(found);
    }

    @Test
    void arrowSchemaRoundTrip() {
        TableSchema.Builder builder = TableSchema.builder().key("id", DType.UTF8);
        for (DType dtype : DType.values()) {
            builder.column(dtype.name(), ColumnSchema.of(dtype).nullable(dtype.ordinal() % 2 == 0));
        }
        TableSchema schema = builder.build();
        assertEquals(schema, TableSchema.of(schema.toArrowSchema(), "id"));
    }

    @Test
    void compoundKeyKeepsDeclarationOrder() {
        TableSchema schema = TableSchema.builder()
                .column("score", DType.FLOAT32)
                .key("user", DType.UTF8)
                .column("rank", ColumnSchema.of(DType.FLOAT32).strict(false))
                .key("item", DType.INT64)
                .build();
        assertEquals(List.of("user", "item"), schema.keyColumns());
        assertEquals(List.of("score", "rank"), schema.valueColumns());

        TableSchema fetched = join(createTable("compound_schema", schema).schema());
        assertEquals(schema, fetched);
        assertEquals(List.of("user", "item"), fetched.keyColumns());
        assertEquals(List.of("score", "user", "rank", "item"), List.copyOf(fetched.columns().keySet()));
        assertTrue(fetched.column("score").strict());
        assertFalse(fetched.column("rank").strict());
    }

    @Test
    void keyOrderIsPartOfTheSchema() {
        TableSchema userFirst = TableSchema.builder().key("user", DType.UTF8).key("item", DType.INT64).build();
        TableSchema itemFirst = TableSchema.builder().key("item", DType.INT64).key("user", DType.UTF8).build();
        assertNotEquals(userFirst, itemFirst);
    }

    static Stream<DType> notKeyable() {
        return Stream.of(DType.BOOL, DType.FLOAT32, DType.FLOAT64);
    }

    @ParameterizedTest
    @MethodSource("notKeyable")
    void keyRejectsTypesTheServerCannotLookUp(DType dtype) {
        assertThrows(IllegalArgumentException.class, () -> TableSchema.builder().key("id", dtype));
    }

    @Test
    void schemaNeedsANonNullableKey() {
        assertThrows(IllegalStateException.class, () -> TableSchema.builder().column("x", DType.INT32).build());
        assertThrows(IllegalArgumentException.class, () -> ColumnSchema.key(DType.UTF8).nullable(true));
    }
}
