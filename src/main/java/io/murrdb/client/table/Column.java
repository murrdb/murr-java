package io.murrdb.client.table;

import io.murrdb.client.table.column.Utf8Column;
import io.murrdb.client.table.column.BoolColumn;
import io.murrdb.client.table.column.Int8Column;
import io.murrdb.client.table.column.Int16Column;
import io.murrdb.client.table.column.Int32Column;
import io.murrdb.client.table.column.Int64Column;
import io.murrdb.client.table.column.UInt8Column;
import io.murrdb.client.table.column.UInt16Column;
import io.murrdb.client.table.column.UInt32Column;
import io.murrdb.client.table.column.UInt64Column;
import io.murrdb.client.table.column.Float32Column;
import io.murrdb.client.table.column.Float64Column;
import java.util.Objects;
import org.apache.arrow.vector.ValueVector;

/**
 * A typed, read-only view over one Arrow vector in a fetch result. Subclasses add the accessors for
 * their JVM primitive. Views read the buffers in place and do not own them, so they are only valid
 * while the result they came from is open.
 */
public abstract sealed class Column
        permits Utf8Column, BoolColumn,
                Int8Column, Int16Column,
                Int32Column, Int64Column,
                UInt8Column, UInt16Column,
                UInt32Column, UInt64Column,
                Float32Column, Float64Column {

    private final DType dtype;
    private final ValueVector vector;

    protected Column(DType dtype, ValueVector vector) {
        this.dtype = dtype;
        this.vector = Objects.requireNonNull(vector, "vector");
    }

    /** Column name as requested in the fetch. */
    public final String name() {
        return vector.getName();
    }

    /** The murr dtype of this column. */
    public final DType dtype() {
        return dtype;
    }

    /** Number of rows, equal to the number of keys requested. */
    public final int size() {
        return vector.getValueCount();
    }

    /** True if the value at {@code row} is null, including rows for keys the server did not find. */
    public final boolean isNull(int row) {
        return vector.isNull(row);
    }

    /** How many rows are null. */
    public final int nullCount() {
        return vector.getNullCount();
    }

    /** The Arrow vector behind this view. Ownership stays with the result. */
    public final ValueVector vector() {
        return vector;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + name() + ", size=" + size() + ", nulls=" + nullCount() + "}";
    }
}
