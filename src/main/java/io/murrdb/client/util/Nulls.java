package io.murrdb.client.util;

import java.util.BitSet;

/** Helpers for the null masks a {@code Batch} takes. Meant to be statically imported. */
public final class Nulls {

    private Nulls() {}

    /** A mask with the given row indexes set to null, so {@code nullsAt(1, 4)} nulls rows 1 and 4. */
    public static BitSet nullsAt(int... rows) {
        BitSet mask = new BitSet();
        for (int row : rows) {
            mask.set(row);
        }
        return mask;
    }
}
