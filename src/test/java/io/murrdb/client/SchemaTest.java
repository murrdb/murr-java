package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void arrowSchemaRoundTrip() {
        TableSchema.Builder builder = TableSchema.builder().key("id");
        for (DType dtype : DType.values()) {
            builder.column(dtype.name(), dtype, dtype.ordinal() % 2 == 0 ? Nullability.NULLABLE : Nullability.NOT_NULL);
        }
        TableSchema schema = builder.build();
        assertEquals(schema, TableSchema.of("id", schema.toArrowSchema()));
    }
}
