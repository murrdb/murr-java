package io.murrdb.client.table;

import org.apache.arrow.vector.types.FloatingPointPrecision;
import org.apache.arrow.vector.types.pojo.ArrowType;

/** Column types murr understands, named exactly as they appear on the wire. */
public enum DType {
    UTF8("utf8"),
    BOOL("bool"),
    INT8("int8"),
    INT16("int16"),
    INT32("int32"),
    INT64("int64"),
    UINT8("uint8"),
    UINT16("uint16"),
    UINT32("uint32"),
    UINT64("uint64"),
    FLOAT32("float32"),
    FLOAT64("float64");

    private final String wireName;

    DType(String wireName) {
        this.wireName = wireName;
    }

    /** The lowercase name used in schema JSON. */
    public String wireName() {
        return wireName;
    }

    /** Parses a wire name. Throws {@link IllegalArgumentException} for anything else. */
    public static DType fromWireName(String name) {
        for (DType t : values()) {
            if (t.wireName.equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("unknown dtype: " + name);
    }

    /** The Arrow type this dtype is sent as. */
    public ArrowType toArrowType() {
        return switch (this) {
            case UTF8 -> ArrowType.Utf8.INSTANCE;
            case BOOL -> ArrowType.Bool.INSTANCE;
            case INT8 -> new ArrowType.Int(8, true);
            case INT16 -> new ArrowType.Int(16, true);
            case INT32 -> new ArrowType.Int(32, true);
            case INT64 -> new ArrowType.Int(64, true);
            case UINT8 -> new ArrowType.Int(8, false);
            case UINT16 -> new ArrowType.Int(16, false);
            case UINT32 -> new ArrowType.Int(32, false);
            case UINT64 -> new ArrowType.Int(64, false);
            case FLOAT32 -> new ArrowType.FloatingPoint(FloatingPointPrecision.SINGLE);
            case FLOAT64 -> new ArrowType.FloatingPoint(FloatingPointPrecision.DOUBLE);
        };
    }

    /** The dtype for an Arrow type, or {@link IllegalArgumentException} if murr has no equivalent. */
    public static DType fromArrowType(ArrowType type) {
        return switch (type) {
            case ArrowType.Utf8 u -> UTF8;
            case ArrowType.Bool b -> BOOL;
            case ArrowType.Int i when i.getBitWidth() == 8 -> i.getIsSigned() ? INT8 : UINT8;
            case ArrowType.Int i when i.getBitWidth() == 16 -> i.getIsSigned() ? INT16 : UINT16;
            case ArrowType.Int i when i.getBitWidth() == 32 -> i.getIsSigned() ? INT32 : UINT32;
            case ArrowType.Int i when i.getBitWidth() == 64 -> i.getIsSigned() ? INT64 : UINT64;
            case ArrowType.FloatingPoint f when f.getPrecision() == FloatingPointPrecision.SINGLE -> FLOAT32;
            case ArrowType.FloatingPoint f when f.getPrecision() == FloatingPointPrecision.DOUBLE -> FLOAT64;
            default -> throw new IllegalArgumentException("no murr dtype for " + type);
        };
    }
}
