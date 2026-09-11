package io.murrdb.client.table.column;

import io.murrdb.client.error.NullValueException;
import io.murrdb.client.table.Column;
import io.murrdb.client.table.DType;

import org.apache.arrow.vector.Float4Vector;

/** A {@code float32} column read as {@code float}. */
public final class Float32Column extends Column {

    private final Float4Vector vector;

    /** Wraps a vector. Fetch results build these; call it yourself only to view your own Arrow data. */
    public Float32Column(Float4Vector vector) {
        super(DType.FLOAT32, vector);
        this.vector = vector;
    }

    /** The value at {@code row}. Throws {@code NullValueException} if it is null. */
    public float get(int row) {
        if (vector.isNull(row)) {
            throw new NullValueException(name(), row);
        }
        return vector.get(row);
    }

    /** The value at {@code row}, or {@code fallback} if it is null. */
    public float getOrDefault(int row, float fallback) {
        return vector.isNull(row) ? fallback : vector.get(row);
    }

    /** Copies the column into a new array, writing {@code fallback} where the value is null. */
    public float[] toArray(float fallback) {
        float[] out = new float[size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = getOrDefault(i, fallback);
        }
        return out;
    }
}
