package io.murrdb.client;

import static io.murrdb.client.util.Nulls.nullsAt;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Every dtype goes through write, fetch and the typed column view, with a null in the middle row. */
class RoundTripTest extends MurrTest {

    private static final List<String> KEYS = List.of("a", "b", "c");

    @Test
    void utf8() {
        roundTrip(DType.UTF8, b -> b.utf8("v", Arrays.asList("", null, "héllo 🌍")), r -> {
            assertArrayEquals(new String[] {"", "?", "héllo 🌍"}, r.utf8("v").toArray("?"));
            assertArrayEquals("héllo 🌍".getBytes(StandardCharsets.UTF_8), r.utf8("v").getBytes(2));
            assertThrows(NullValueException.class, () -> r.utf8("v").get(1));
            return r.utf8("v");
        });
    }

    @Test
    void bool() {
        roundTrip(DType.BOOL, b -> b.bool("v", new boolean[] {true, false, false}, nullsAt(1)), r -> {
            assertTrue(r.bool("v").get(0));
            assertFalse(r.bool("v").get(2));
            assertTrue(r.bool("v").getOrDefault(1, true));
            assertThrows(NullValueException.class, () -> r.bool("v").get(1));
            return r.bool("v");
        });
    }

    @Test
    void int8() {
        roundTrip(DType.INT8, b -> b.int8("v", new byte[] {Byte.MIN_VALUE, 0, Byte.MAX_VALUE}, nullsAt(1)), r -> {
            assertArrayEquals(new byte[] {Byte.MIN_VALUE, 7, Byte.MAX_VALUE}, r.int8("v").toArray((byte) 7));
            assertThrows(NullValueException.class, () -> r.int8("v").get(1));
            return r.int8("v");
        });
    }

    @Test
    void int16() {
        roundTrip(DType.INT16, b -> b.int16("v", new short[] {Short.MIN_VALUE, 0, Short.MAX_VALUE}, nullsAt(1)), r -> {
            assertArrayEquals(new short[] {Short.MIN_VALUE, 7, Short.MAX_VALUE}, r.int16("v").toArray((short) 7));
            assertThrows(NullValueException.class, () -> r.int16("v").get(1));
            return r.int16("v");
        });
    }

    @Test
    void int32() {
        roundTrip(DType.INT32, b -> b.int32("v", new int[] {Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, nullsAt(1)), r -> {
            assertArrayEquals(new int[] {Integer.MIN_VALUE, 7, Integer.MAX_VALUE}, r.int32("v").toArray(7));
            assertThrows(NullValueException.class, () -> r.int32("v").get(1));
            return r.int32("v");
        });
    }

    @Test
    void int64() {
        roundTrip(DType.INT64, b -> b.int64("v", new long[] {Long.MIN_VALUE, 0, Long.MAX_VALUE}, nullsAt(1)), r -> {
            assertArrayEquals(new long[] {Long.MIN_VALUE, 7, Long.MAX_VALUE}, r.int64("v").toArray(7));
            assertThrows(NullValueException.class, () -> r.int64("v").get(1));
            return r.int64("v");
        });
    }

    @Test
    void uint8() {
        roundTrip(DType.UINT8, b -> b.uint8("v", new short[] {0, 0, 255}, nullsAt(1)), r -> {
            assertArrayEquals(new short[] {0, 7, 255}, r.uint8("v").toArray((short) 7));
            assertThrows(NullValueException.class, () -> r.uint8("v").get(1));
            return r.uint8("v");
        });
    }

    @Test
    void uint16() {
        roundTrip(DType.UINT16, b -> b.uint16("v", new int[] {0, 0, 65535}, nullsAt(1)), r -> {
            assertArrayEquals(new int[] {0, 7, 65535}, r.uint16("v").toArray(7));
            assertThrows(NullValueException.class, () -> r.uint16("v").get(1));
            return r.uint16("v");
        });
    }

    @Test
    void uint32() {
        roundTrip(DType.UINT32, b -> b.uint32("v", new long[] {0, 0, 4294967295L}, nullsAt(1)), r -> {
            assertArrayEquals(new long[] {0, 7, 4294967295L}, r.uint32("v").toArray(7));
            assertThrows(NullValueException.class, () -> r.uint32("v").get(1));
            return r.uint32("v");
        });
    }

    @Test
    void uint64() {
        roundTrip(DType.UINT64, b -> b.uint64("v", new long[] {0, 0, -1L}, nullsAt(1)), r -> {
            assertArrayEquals(new long[] {0, 7, -1L}, r.uint64("v").toArray(7));
            assertEquals("18446744073709551615", Long.toUnsignedString(r.uint64("v").get(2)));
            assertThrows(NullValueException.class, () -> r.uint64("v").get(1));
            return r.uint64("v");
        });
    }

    @Test
    void float32() {
        roundTrip(DType.FLOAT32, b -> b.float32("v", new float[] {Float.NaN, 0f, -0.0f}, nullsAt(1)), r -> {
            assertTrue(Float.isNaN(r.float32("v").get(0)));
            assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(r.float32("v").get(2)));
            assertEquals(7f, r.float32("v").getOrDefault(1, 7f));
            assertThrows(NullValueException.class, () -> r.float32("v").get(1));
            return r.float32("v");
        });
    }

    @Test
    void float64() {
        roundTrip(DType.FLOAT64, b -> b.float64("v", new double[] {Double.NaN, 0, Double.MAX_VALUE}, nullsAt(1)), r -> {
            assertTrue(Double.isNaN(r.float64("v").get(0)));
            assertEquals(Double.MAX_VALUE, r.float64("v").get(2));
            assertEquals(7.0, r.float64("v").getOrDefault(1, 7.0));
            assertThrows(NullValueException.class, () -> r.float64("v").get(1));
            return r.float64("v");
        });
    }

    @Test
    void missingKeysComeBackAsNullRows() {
        TableSchema schema = TableSchema.builder().key("id").column("x", DType.INT32).build();
        Table table = createTable("missing", schema);
        try (Batch batch = Batch.of(schema)
                .utf8("id", List.of("k1", "k2"))
                .int32("x", new int[] {1, 2})
                .build(allocator)) {
            join(table.write(batch));
        }

        join(table.fetch(List.of("k2", "nope", "k1"), List.of("x"), r -> {
            assertEquals(3, r.rowCount());
            assertEquals(List.of("k2", "nope", "k1"), r.keys());
            assertTrue(r.found(0));
            assertFalse(r.found(1));
            assertTrue(r.found(2));
            assertArrayEquals(new int[] {2, -1, 1}, r.int32("x").toArray(-1));
            return null;
        }));
    }

    @Test
    void columnsFollowRequestOrder() {
        TableSchema schema = TableSchema.builder()
                .key("id")
                .column("x", DType.INT32)
                .column("y", DType.UTF8)
                .column("z", DType.BOOL)
                .build();
        Table table = createTable("order", schema);
        try (Batch batch = Batch.of(schema)
                .utf8("id", List.of("k"))
                .int32("x", new int[] {1})
                .utf8("y", List.of("y"))
                .bool("z", new boolean[] {true})
                .build(allocator)) {
            join(table.write(batch));
        }

        join(table.fetch(List.of("k"), List.of("z", "x"), r -> {
            assertEquals(List.of("z", "x"), r.columns());
            assertTrue(r.bool("z").get(0));
            assertEquals(1, r.int32("x").get(0));
            return null;
        }));
    }

    @Test
    void secondWriteOverwritesTheFirst() {
        TableSchema schema = TableSchema.builder().key("id").column("x", DType.INT32).column("y", DType.UTF8).build();
        Table table = createTable("overwrite", schema);
        try (Batch first = Batch.of(schema)
                        .utf8("id", List.of("k"))
                        .int32("x", new int[] {1})
                        .utf8("y", List.of("old"))
                        .build(allocator);
                Batch second = Batch.of(schema)
                        .utf8("id", List.of("k"))
                        .int32("x", new int[] {2})
                        .utf8("y", List.of("new"))
                        .build(allocator)) {
            join(table.write(first));
            join(table.write(second));
        }

        join(table.fetch(List.of("k"), List.of("x", "y"), r -> {
            assertEquals(2, r.int32("x").get(0));
            assertEquals("new", r.utf8("y").get(0));
            return null;
        }));
    }

    @Test
    void largeBatchSurvivesVectorReallocation() {
        TableSchema schema = TableSchema.builder().key("id").column("s", DType.UTF8).column("f", DType.FLOAT64).build();
        Table table = createTable("large", schema);
        int rows = 10_000;
        Random random = new Random(42);
        List<String> ids = IntStream.range(0, rows).mapToObj(i -> "key" + i).toList();
        List<String> strings = new ArrayList<>(rows);
        double[] doubles = new double[rows];
        for (int i = 0; i < rows; i++) {
            strings.add("v".repeat(random.nextInt(200)) + i);
            doubles[i] = random.nextDouble();
        }
        try (Batch batch = Batch.of(schema).utf8("id", ids).utf8("s", strings).float64("f", doubles).build(allocator)) {
            assertEquals(rows, batch.rowCount());
            join(table.write(batch));
        }

        List<String> sample = IntStream.range(0, 500).map(i -> i * 20 + 7).mapToObj(i -> "key" + i).toList();
        join(table.fetch(sample, List.of("s", "f"), r -> {
            for (int i = 0; i < sample.size(); i++) {
                int source = i * 20 + 7;
                assertEquals(strings.get(source), r.utf8("s").get(i));
                assertEquals(doubles[source], r.float64("f").get(i));
            }
            return null;
        }));
    }

    @Test
    void omittedNullableColumnReadsAsNull() {
        TableSchema schema = TableSchema.builder().key("id").column("x", DType.INT32).column("y", DType.UTF8).build();
        Table table = createTable("omitted", schema);
        try (Batch batch = Batch.of(schema).utf8("id", List.of("k")).int32("x", new int[] {1}).build(allocator)) {
            join(table.write(batch));
        }

        join(table.fetch(List.of("k"), List.of("x", "y"), r -> {
            assertTrue(r.found(0));
            assertEquals(1, r.int32("x").get(0));
            assertTrue(r.utf8("y").isNull(0));
            return null;
        }));
    }

    /** Table with key {@code id} and one nullable column {@code v}, three rows, then fetch in the same order. */
    private void roundTrip(DType dtype, UnaryOperator<Batch.Builder> fill, Function<FetchResult, Column> check) {
        TableSchema schema = TableSchema.builder().key("id").column("v", dtype).build();
        Table table = createTable(dtype.name().toLowerCase(), schema);
        try (Batch batch = fill.apply(Batch.of(schema).utf8("id", KEYS)).build(allocator)) {
            join(table.write(batch));
        }
        join(table.fetch(KEYS, List.of("v"), r -> {
            assertEquals(KEYS, r.keys());
            Column column = check.apply(r);
            assertEquals(dtype, column.dtype());
            assertEquals(3, column.size());
            assertEquals(1, column.nullCount());
            assertTrue(column.isNull(1));
            return null;
        }));
    }
}
