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
import org.apache.arrow.vector.types.pojo.ArrowType;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.FieldType;
import org.apache.arrow.vector.types.pojo.Schema;

/**
 * A murr table schema: one utf8 key column plus any number of typed columns, in declaration order.
 * Immutable. Build one with {@link #builder()} or convert from an Arrow schema with {@link #of}.
 */
public final class TableSchema {

    private final String key;
    private final Map<String, ColumnSchema> columns;

    private TableSchema(String key, LinkedHashMap<String, ColumnSchema> columns) {
        this.key = key;
        this.columns = Collections.unmodifiableMap(columns);
    }

    /** Starts a schema. Call {@link Builder#key} before {@link Builder#build}. */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a schema from an Arrow schema, taking {@code keyColumn} as the key.
     * Fails with {@link IllegalArgumentException} if the key field is missing, nullable, not utf8, or a
     * field type has no murr dtype.
     */
    public static TableSchema of(String keyColumn, Schema arrowSchema) {
        Objects.requireNonNull(keyColumn, "keyColumn");
        Objects.requireNonNull(arrowSchema, "arrowSchema");
        Field keyField = arrowSchema.findField(keyColumn);
        if (keyField == null) {
            throw new IllegalArgumentException("no field " + keyColumn + " in " + arrowSchema);
        }
        if (!(keyField.getType() instanceof ArrowType.Utf8) || keyField.isNullable()) {
            throw new IllegalArgumentException("key " + keyColumn + " must be non-nullable utf8, got " + keyField);
        }
        Builder b = builder().key(keyColumn);
        for (Field f : arrowSchema.getFields()) {
            if (f.getName().equals(keyColumn)) {
                continue;
            }
            b.column(f.getName(), DType.fromArrowType(f.getType()),
                    f.isNullable() ? Nullability.NULLABLE : Nullability.NOT_NULL);
        }
        return b.build();
    }

    /** Name of the key column. */
    public String key() {
        return key;
    }

    /** All columns including the key, in declaration order, as the server stores them. */
    public Map<String, ColumnSchema> columns() {
        return columns;
    }

    /** The column definition, or {@code null} if there is no such column. */
    public ColumnSchema column(String name) {
        return columns.get(name);
    }

    /** The Arrow schema for record batches written to this table, key column first. */
    public Schema toArrowSchema() {
        List<Field> fields = new ArrayList<>(columns.size());
        for (Map.Entry<String, ColumnSchema> e : columns.entrySet()) {
            ColumnSchema c = e.getValue();
            fields.add(new Field(e.getKey(), new FieldType(c.nullable(), c.dtype().toArrowType(), null), null));
        }
        return new Schema(fields);
    }

    /** The schema as the server expects it: {@code {"key":"id","columns":{"id":{"dtype":"utf8","nullable":false},...}}}. */
    public byte[] toJson() {
        ObjectNode root = Json.MAPPER.createObjectNode();
        root.put("key", key);
        ObjectNode cols = root.putObject("columns");
        for (Map.Entry<String, ColumnSchema> e : columns.entrySet()) {
            cols.putObject(e.getKey())
                    .put("dtype", e.getValue().dtype().wireName())
                    .put("nullable", e.getValue().nullable());
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
        JsonNode key = node.get("key");
        JsonNode cols = node.get("columns");
        if (key == null || !key.isTextual() || cols == null || !cols.isObject()) {
            throw new IllegalArgumentException("not a table schema: " + node);
        }
        Builder b = builder().key(key.asText());
        cols.properties().forEach(e -> {
            if (e.getKey().equals(key.asText())) {
                return;
            }
            JsonNode dtype = e.getValue().get("dtype");
            if (dtype == null || !dtype.isTextual()) {
                throw new IllegalArgumentException("column " + e.getKey() + " has no dtype");
            }
            JsonNode nullable = e.getValue().get("nullable");
            boolean isNullable = nullable == null || nullable.asBoolean(true);
            b.column(e.getKey(), DType.fromWireName(dtype.asText()),
                    isNullable ? Nullability.NULLABLE : Nullability.NOT_NULL);
        });
        return b.build();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TableSchema other && key.equals(other.key) && columns.equals(other.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, columns);
    }

    @Override
    public String toString() {
        return "TableSchema{key=" + key + ", columns=" + columns + "}";
    }

    /** Collects columns for a {@link TableSchema}. Not thread-safe. */
    public static final class Builder {

        private String key;
        private final LinkedHashMap<String, ColumnSchema> columns = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Names the key column. It is always utf8 and non-nullable, because the server looks rows up
         * by string keys and refuses null ones. Exactly one key per table.
         */
        public Builder key(String name) {
            Objects.requireNonNull(name, "name");
            if (key != null) {
                throw new IllegalStateException("key already set to " + key);
            }
            if (columns.containsKey(name)) {
                throw new IllegalArgumentException("column already defined: " + name);
            }
            key = name;
            LinkedHashMap<String, ColumnSchema> reordered = new LinkedHashMap<>();
            reordered.put(name, new ColumnSchema(DType.UTF8, false));
            reordered.putAll(columns);
            columns.clear();
            columns.putAll(reordered);
            return this;
        }

        /** Adds a nullable column. */
        public Builder column(String name, DType dtype) {
            return column(name, dtype, Nullability.NULLABLE);
        }

        /** Adds a column. Rejects duplicate names. */
        public Builder column(String name, DType dtype, Nullability nullability) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(dtype, "dtype");
            Objects.requireNonNull(nullability, "nullability");
            if (columns.containsKey(name)) {
                throw new IllegalArgumentException("column already defined: " + name);
            }
            columns.put(name, new ColumnSchema(dtype, nullability == Nullability.NULLABLE));
            return this;
        }

        /** Fails with {@link IllegalStateException} if no key was named. */
        public TableSchema build() {
            if (key == null) {
                throw new IllegalStateException("schema has no key column, call key(name)");
            }
            return new TableSchema(key, new LinkedHashMap<>(columns));
        }
    }
}
