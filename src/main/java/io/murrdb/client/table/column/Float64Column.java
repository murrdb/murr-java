package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
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

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public double get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public double getOrDefault(int row, double fallback) {
        return vector.isNull(row) ? fallback : vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public double[] toArray(double fallback) {
        double[] out = new double[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
