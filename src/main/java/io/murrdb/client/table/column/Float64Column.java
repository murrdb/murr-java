package io.murrdb.client.table.column;

import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.Float8Vector;

/** A {@code float64} column read as {@code double}. */
public final class Float64Column extends Column {

    private final Float8Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Float64Column(Float8Vector vector) {
        super(DType.FLOAT64, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. Not implemented yet. */
    public double get(int row) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** The value at {@code row}, or {@code fallback} if it is null. Not implemented yet. */
    public double getOrDefault(int row, double fallback) {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. Not implemented yet. */
    public double[] toArray(double fallback) {
        throw new UnsupportedOperationException("not implemented");
    }
}
