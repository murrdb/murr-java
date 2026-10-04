package io.murrdb.client.table;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.murrdb.client.internal.Json;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * A murr table schema: typed columns in declaration order, at least one of them a key. Several key
 * columns form a compound key, ordered as declared. Immutable. Build one with {@link #builder()} or
 * convert from an Arrow schema with {@link #of}.
 */
public final class TableSchema {

    private final Map<String, ColumnSchema> columns;
    private final List<String> keyColumns;
    private final List<String> valueColumns;

    private TableSchema(LinkedHashMap<String, ColumnSchema> columns) {
        this.columns = Collections.unmodifiableMap(columns);
        this.keyColumns = columns.entrySet().stream().filter(e -> e.getValue().key()).map(Map.Entry::getKey).toList();
        this.valueColumns = columns.entrySet().stream().filter(e -> !e.getValue().key()).map(Map.Entry::getKey).toList();
    }

    /** Starts a schema. Add at least one {@link Builder#key} before {@link Builder#build}. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a schema from an Arrow schema, taking {@code keyColumns} as the key. Fields keep their order.
     * Fails with {@link IllegalArgumentException} if a key field is missing, nullable or of a type that
     * cannot be a key, or a field type has no murr dtype.
     */
    public static TableSchema of(Schema arrowSchema, String... keyColumns) {
        Objects.requireNonNull(arrowSchema, "arrowSchema");
        List<String> keys = List.of(keyColumns);
        for (String key : keys) {
            if (arrowSchema.findField(key) == null) {
                throw new IllegalArgumentException("no field " + key + " in " + arrowSchema);
            }
        }
        Builder b = builder();
        for (Field f : arrowSchema.getFields()) {
            DType dtype = DType.fromArrowType(f.getType());
            if (!keys.contains(f.getName())) {
                b.column(f.getName(), ColumnSchema.of(dtype).nullable(f.isNullable()));
            } else if (f.isNullable()) {
                throw new IllegalArgumentException("key " + f.getName() + " must be non-nullable, got " + f);
            } else {
                b.key(f.getName(), dtype);
            }
        }
        return b.build();
    }

    /** Names of the key columns, in declaration order. Never empty. */
    public List<String> keyColumns() {
        return keyColumns;
    }

    /** Names of the columns that are not keys, in declaration order. Only these can be fetched. */
    public List<String> valueColumns() {
        return valueColumns;
    }

    /** All columns including the keys, in declaration order, as the server stores them. */
    public Map<String, ColumnSchema> columns() {
        return columns;
    }

    /** The column definition, or {@code null} if there is no such column. */
    public ColumnSchema column(String name) {
        return columns.get(name);
    }

    /** The Arrow schema for record batches written to this table, in declaration order. */
    public Schema toArrowSchema() {
        List<Field> fields = new ArrayList<>(columns.size());
        for (Map.Entry<String, ColumnSchema> e : columns.entrySet()) {
            ColumnSchema c = e.getValue();
            fields.add(new Field(e.getKey(), new FieldType(c.nullable(), c.dtype().toArrowType(), null), null));
        }
        return new Schema(fields);
    }

    /** The schema as the server expects it: {@code {"columns":{"id":{"dtype":"utf8","nullable":false,"key":true,"strict":true},...}}}. */
    public byte[] toJson() {
        ObjectNode root = Json.MAPPER.createObjectNode();
        ObjectNode cols = root.putObject("columns");
        for (Map.Entry<String, ColumnSchema> e : columns.entrySet()) {
            ColumnSchema c = e.getValue();
            cols.putObject(e.getKey())
                    .put("dtype", c.dtype().wireName())
                    .put("nullable", c.nullable())
                    .put("key", c.key())
                    .put("strict", c.strict());
        }
        return Json.write(root);
    }

    /** Parses one schema as the server sends it. Throws {@link IllegalArgumentException} on anything else. */
    public static TableSchema fromJson(byte[] json) {
        return fromNode(Json.read(json));
    }

    /** Parses the table listing, a map from table name to schema, in server order. */
    public static Map<String, TableSchema> mapFromJson(byte[] json) {
        Map<String, TableSchema> out = new LinkedHashMap<>();
        Json.read(json).properties().forEach(e -> out.put(e.getKey(), fromNode(e.getValue())));
        return out;
    }

    private static TableSchema fromNode(JsonNode node) {
        JsonNode cols = node.get("columns");
        if (cols == null || !cols.isObject()) {
            throw new IllegalArgumentException("not a table schema: " + node);
        }
        Builder b = builder();
        cols.properties().forEach(e -> {
            JsonNode c = e.getValue();
            JsonNode dtype = c.get("dtype");
            if (dtype == null || !dtype.isTextual()) {
                throw new IllegalArgumentException("column " + e.getKey() + " has no dtype");
            }
            // Missing flags take the server defaults: nullable, not a key, strict.
            b.column(e.getKey(), new ColumnSchema(DType.fromWireName(dtype.asText()),
                    c.path("nullable").asBoolean(true), c.path("key").asBoolean(false), c.path("strict").asBoolean(true)));
        });
        return b.build();
    }

    // Map equality ignores order, and order is part of a schema: it decides the compound key.
    @Override
    public boolean equals(Object o) {
        return o instanceof TableSchema other
                && columns.equals(other.columns)
                && List.copyOf(columns.keySet()).equals(List.copyOf(other.columns.keySet()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(columns, List.copyOf(columns.keySet()));
    }

    @Override
    public String toString() {
        return "TableSchema{" + columns + "}";
    }

    /** Collects columns for a {@link TableSchema}, in the order they are added. Not thread-safe. */
    public static final class Builder {

        private final LinkedHashMap<String, ColumnSchema> columns = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Adds a key column, which is never nullable. Calling this more than once makes a compound key
         * whose parts are ordered as added. Rejects dtypes that cannot be keys: bool and the floats.
         */
        public Builder key(String name, DType dtype) {
            return column(name, ColumnSchema.key(dtype));
        }

        /** Adds a nullable, strict column: the server defaults. */
        public Builder column(String name, DType dtype) {
            return column(name, ColumnSchema.of(dtype));
        }

        /** Adds a column. Rejects duplicate names. */
        public Builder column(String name, ColumnSchema column) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(column, "column");
            if (columns.containsKey(name)) {
                throw new IllegalArgumentException("column already defined: " + name);
            }
            columns.put(name, column);
            return this;
        }

        /** Fails with {@link IllegalStateException} if there is no key column. */
        public TableSchema build() {
            if (columns.values().stream().noneMatch(ColumnSchema::key)) {
                throw new IllegalStateException("schema has no key column, call key(name, dtype)");
            }
            return new TableSchema(new LinkedHashMap<>(columns));
        }
    }
}
