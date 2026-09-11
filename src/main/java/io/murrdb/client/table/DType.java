package io.murrdb.client.table;

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

    /** The Arrow type this dtype is sent as. Not implemented yet. */
    public ArrowType toArrowType() {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The dtype for an Arrow type, or {@link IllegalArgumentException} if murr has no equivalent. Not implemented yet. */
    public static DType fromArrowType(ArrowType type) {
        throw new UnsupportedOperationException("not implemented");
    }
}
