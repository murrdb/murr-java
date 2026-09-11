package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.murrdb.client.error.ColumnTypeException;
import io.murrdb.client.error.MurrException;
import io.murrdb.client.error.MurrRequestException;
import io.murrdb.client.error.NullValueException;
import io.murrdb.client.error.TableAlreadyExistsException;
import io.murrdb.client.error.TableNotFoundException;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.Nullability;
import io.murrdb.client.table.TableSchema;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ErrorTest extends MurrTest {

    private static final TableSchema SCHEMA = TableSchema.builder()
            .key("id")
            .column("x", DType.INT32, Nullability.NOT_NULL)
            .column("y", DType.UTF8)
            .build();

    @Test
    void missingTable() {
        TableNotFoundException e = assertThrows(TableNotFoundException.class,
                () -> join(client.table("no_such_table").fetch(List.of("k"), List.of("x"))));
        assertEquals("no_such_table", e.table());
    }

    @Test
    void dropMissingTable() {
        TableNotFoundException e = assertThrows(TableNotFoundException.class,
                () -> join(client.dropTable("no_such_table")));
        assertEquals("no_such_table", e.table());
    }

    @Test
    void duplicateTable() {
        Table table = createTable("dup", SCHEMA);
        TableAlreadyExistsException e = assertThrows(TableAlreadyExistsException.class,
                () -> join(client.createTable(table.name(), SCHEMA)));
        assertEquals(table.name(), e.table());
    }

    @Test
    void keyColumnCannotBeFetched() {
        Table table = createTable("keyfetch", SCHEMA);
        assertThrows(MurrRequestException.class, () -> join(table.fetch(List.of("k"), List.of("id", "x"))));
    }

    @Test
    void unknownColumnCannotBeFetched() {
        Table table = createTable("badcol", SCHEMA);
        assertThrows(MurrRequestException.class, () -> join(table.fetch(List.of("k"), List.of("nope"))));
    }

    @Test
    void writeWithForeignSchemaIsRejected() {
        Table table = createTable("badwrite", SCHEMA);
        TableSchema other = TableSchema.builder().key("id").column("x", DType.UTF8).build();
        try (Batch batch = Batch.of(other).utf8("id", List.of("k")).utf8("x", List.of("v")).build(allocator)) {
            // murr 0.2.1 answers this with a 500 from the Arrow layer rather than a 400
            assertThrows(MurrException.class, () -> join(table.write(batch)));
        }
    }

    @Test
    void failingFunctionFailsTheFutureAndStillClosesTheResult() {
        Table table = createTable("fn", SCHEMA);
        try (Batch batch = Batch.of(SCHEMA).utf8("id", List.of("k")).int32("x", new int[] {1}).build(allocator)) {
            join(table.write(batch));
        }
        RuntimeException e = assertThrows(RuntimeException.class,
                () -> join(table.fetch(List.of("k"), List.of("x"), r -> {
                    throw new IllegalStateException("boom");
                })));
        assertInstanceOf(IllegalStateException.class, e);
        assertEquals("boom", e.getMessage());
    }

    static Stream<Arguments> rejectedBatches() {
        return Stream.of(
                Arguments.of("dtype mismatch", ColumnTypeException.class,
                        (UnaryOperator<Batch.Builder>) b -> b.utf8("id", List.of("k")).int64("x", new long[] {1})),
                Arguments.of("null key", NullValueException.class,
                        (UnaryOperator<Batch.Builder>) b -> b.utf8("id", Arrays.asList("k", null)).int32("x", new int[] {1, 2})),
                Arguments.of("missing non-nullable column", IllegalArgumentException.class,
                        (UnaryOperator<Batch.Builder>) b -> b.utf8("id", List.of("k"))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rejectedBatches")
    void builderRejects(String name, Class<? extends RuntimeException> expected, UnaryOperator<Batch.Builder> fill) {
        assertThrows(expected, () -> {
            try (Batch batch = fill.apply(Batch.of(SCHEMA)).build(allocator)) {
                assertEquals(0, batch.rowCount(), name + " should not have built");
            }
        });
    }
}
