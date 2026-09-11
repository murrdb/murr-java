package io.murrdb.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.murrdb.client.table.DType;
import io.murrdb.client.table.TableSchema;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import org.apache.arrow.vector.BigIntVector;
import org.apache.arrow.vector.BitVector;
import org.apache.arrow.vector.FieldVector;
import org.apache.arrow.vector.Float4Vector;
import org.apache.arrow.vector.Float8Vector;
import org.apache.arrow.vector.IntVector;
import org.apache.arrow.vector.SmallIntVector;
import org.apache.arrow.vector.TinyIntVector;
import org.apache.arrow.vector.UInt1Vector;
import org.apache.arrow.vector.UInt2Vector;
import org.apache.arrow.vector.UInt4Vector;
import org.apache.arrow.vector.UInt8Vector;
import org.apache.arrow.vector.VarCharVector;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;

/**
 * Writes hand-built Arrow roots, the path a caller with existing Arrow data takes. Each dtype gets a
 * root of key plus one column of that type, with a value in row 0 and a null in row 1.
 */
class WriteRootTest extends MurrTest {

    @Test
    void utf8() {
        rawRoot(DType.UTF8, VarCharVector.class, v -> v.setSafe(0, "héllo".getBytes(StandardCharsets.UTF_8)),
                r -> assertEquals("héllo", r.utf8("v").get(1)));
    }

    @Test
    void bool() {
        rawRoot(DType.BOOL, BitVector.class, v -> v.setSafe(0, 1), r -> assertTrue(r.bool("v").get(1)));
    }

    @Test
    void int8() {
        rawRoot(DType.INT8, TinyIntVector.class, v -> v.setSafe(0, Byte.MIN_VALUE),
                r -> assertEquals(Byte.MIN_VALUE, r.int8("v").get(1)));
    }

    @Test
    void int16() {
        rawRoot(DType.INT16, SmallIntVector.class, v -> v.setSafe(0, Short.MIN_VALUE),
                r -> assertEquals(Short.MIN_VALUE, r.int16("v").get(1)));
    }

    @Test
    void int32() {
        rawRoot(DType.INT32, IntVector.class, v -> v.setSafe(0, Integer.MIN_VALUE),
                r -> assertEquals(Integer.MIN_VALUE, r.int32("v").get(1)));
    }

    @Test
    void int64() {
        rawRoot(DType.INT64, BigIntVector.class, v -> v.setSafe(0, Long.MIN_VALUE),
                r -> assertEquals(Long.MIN_VALUE, r.int64("v").get(1)));
    }

    @Test
    void uint8() {
        rawRoot(DType.UINT8, UInt1Vector.class, v -> v.setSafe(0, (byte) 0xFF),
                r -> assertEquals((short) 255, r.uint8("v").get(1)));
    }

    @Test
    void uint16() {
        rawRoot(DType.UINT16, UInt2Vector.class, v -> v.setSafe(0, (char) 0xFFFF),
                r -> assertEquals(65535, r.uint16("v").get(1)));
    }

    @Test
    void uint32() {
        rawRoot(DType.UINT32, UInt4Vector.class, v -> v.setSafe(0, 0xFFFFFFFF),
                r -> assertEquals(4294967295L, r.uint32("v").get(1)));
    }

    @Test
    void uint64() {
        rawRoot(DType.UINT64, UInt8Vector.class, v -> v.setSafe(0, -1L), r -> assertEquals(-1L, r.uint64("v").get(1)));
    }

    @Test
    void float32() {
        rawRoot(DType.FLOAT32, Float4Vector.class, v -> v.setSafe(0, 1.5f),
                r -> assertEquals(1.5f, r.float32("v").get(1)));
    }

    @Test
    void float64() {
        rawRoot(DType.FLOAT64, Float8Vector.class, v -> v.setSafe(0, Double.MAX_VALUE),
                r -> assertEquals(Double.MAX_VALUE, r.float64("v").get(1)));
    }

    @Test
    void rootColumnsMatchByNameNotPosition() {
        TableSchema schema = TableSchema.builder().key("id").column("x", DType.INT32).column("s", DType.UTF8).build();
        Table table = createTable("root", schema);

        Schema reordered = new Schema(List.of(
                new Field("s", FieldType.nullable(ArrowType.Utf8.INSTANCE), null),
                new Field("x", FieldType.nullable(new ArrowType.Int(32, true)), null),
                new Field("id", FieldType.nullable(ArrowType.Utf8.INSTANCE), null)));
        try (VectorSchemaRoot root = VectorSchemaRoot.create(reordered, allocator)) {
            VarCharVector s = (VarCharVector) root.getVector("s");
            IntVector x = (IntVector) root.getVector("x");
            VarCharVector id = (VarCharVector) root.getVector("id");
            root.allocateNew();
            for (int i = 0; i < 3; i++) {
                s.setSafe(i, ("s" + i).getBytes(StandardCharsets.UTF_8));
                x.setSafe(i, i * 10);
                id.setSafe(i, ("k" + i).getBytes(StandardCharsets.UTF_8));
            }
            root.setRowCount(3);

            join(table.write(root));

            assertEquals(3, root.getRowCount());
            assertEquals(20, x.get(2));
        }

        join(table.fetch(List.of("k2", "k0"), List.of("x", "s"), r -> {
            assertEquals(20, r.int32("x").get(0));
            assertEquals("s2", r.utf8("s").get(0));
            assertEquals(0, r.int32("x").get(1));
            assertEquals("s0", r.utf8("s").get(1));
            return null;
        }));
    }

    /** Writes rows k0 (value set by {@code fill}) and k1 (null), then fetches them in reverse order. */
    private <V extends FieldVector> void rawRoot(DType dtype, Class<V> type, Consumer<V> fill, Consumer<FetchResult> check) {
        TableSchema schema = TableSchema.builder().key("id").column("v", dtype).build();
        Table table = createTable("raw_" + dtype.name().toLowerCase(), schema);

        try (VectorSchemaRoot root = VectorSchemaRoot.create(schema.toArrowSchema(), allocator)) {
            root.allocateNew();
            VarCharVector id = (VarCharVector) root.getVector("id");
            id.setSafe(0, "k0".getBytes(StandardCharsets.UTF_8));
            id.setSafe(1, "k1".getBytes(StandardCharsets.UTF_8));
            V v = type.cast(root.getVector("v"));
            fill.accept(v);
            v.setNull(1);
            root.setRowCount(2);
            join(table.write(root));
        }

        join(table.fetch(List.of("k1", "k0"), List.of("v"), r -> {
            assertTrue(r.root().getVector("v").isNull(0));
            check.accept(r);
            return null;
        }));
    }
}
