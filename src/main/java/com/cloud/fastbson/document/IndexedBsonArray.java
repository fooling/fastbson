package com.cloud.fastbson.document;

import com.cloud.fastbson.handler.parsers.*;
import com.cloud.fastbson.util.BsonType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zero-copy BSON array implementation with index-based lazy parsing.
 *
 * <p><b>Phase 2.16: Array Support</b>
 *
 * <p>Similar to IndexedBsonDocument, but for arrays:
 * <ul>
 *   <li>Parse phase: Build element index only</li>
 *   <li>Access phase: Lazy parse on demand</li>
 *   <li>Cache: Store parsed values for repeated access</li>
 * </ul>
 *
 * <p>Array format in BSON: same as document with numeric field names ("0", "1", "2", ...)
 */
public class IndexedBsonArray implements BsonArray {
    private final byte[] data;
    private final int offset;
    private final int length;
    private final ElementIndex[] elements;
    private volatile Object[] cache;

    /**
     * Element metadata (similar to FieldIndex but for array elements).
     */
    static class ElementIndex {
        final int valueOffset;
        final int valueSize;
        final byte type;

        ElementIndex(int valueOffset, int valueSize, byte type) {
            this.valueOffset = valueOffset;
            this.valueSize = valueSize;
            this.type = type;
        }
    }

    private IndexedBsonArray(byte[] data, int offset, int length, ElementIndex[] elements) {
        this.data = data;
        this.offset = offset;
        this.length = length;
        this.elements = elements;
    }

    /**
     * Parse BSON array from byte array (zero-copy).
     *
     * @param data BSON array data
     * @param offset array start offset
     * @param length array length
     * @return IndexedBsonArray
     */
    public static IndexedBsonArray parse(byte[] data, int offset, int length) {
        List<ElementIndex> elementList = new ArrayList<>();
        int pos = offset + 4;  // Skip array length
        int endPos = offset + length - 1;  // -1 for terminator

        while (pos < endPos && data[pos] != 0) {
            byte type = data[pos++];

            // Skip field name (array index like "0", "1", "2")
            while (data[pos++] != 0) {
                // Skip until null terminator
            }

            // Get value size
            int valueOffset = pos;
            int valueSize = getValueSize(data, valueOffset, type);

            elementList.add(new ElementIndex(valueOffset, valueSize, type));

            pos += valueSize;
        }

        ElementIndex[] elementArray = elementList.toArray(new ElementIndex[0]);
        return new IndexedBsonArray(data, offset, length, elementArray);
    }

    /**
     * Get value size using appropriate parser.
     */
    private static int getValueSize(byte[] data, int offset, byte type) {
        // Reuse logic from IndexedBsonDocument
        switch (type) {
            case BsonType.DOUBLE:
                return 8;
            case BsonType.STRING:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
                return StringParser.INSTANCE.getValueSize(data, offset);
            case BsonType.DOCUMENT:
                return DocumentParser.INSTANCE.getValueSize(data, offset);
            case BsonType.ARRAY:
                return ArrayParser.INSTANCE.getValueSize(data, offset);
            case BsonType.BINARY:
                int binLength = Int32Parser.readDirect(data, offset);
                return 4 + 1 + binLength;
            case BsonType.OBJECT_ID:
                return 12;
            case BsonType.BOOLEAN:
                return 1;
            case BsonType.DATE_TIME:
                return 8;
            case BsonType.NULL:
            case BsonType.UNDEFINED:
                return 0;
            case BsonType.REGEX:
                int patternLen = 0;
                int p = offset;
                while (data[p++] != 0) patternLen++;
                int optionsLen = 0;
                while (data[p++] != 0) optionsLen++;
                return patternLen + 1 + optionsLen + 1;
            case BsonType.INT32:
                return 4;
            case BsonType.TIMESTAMP:
                return 8;
            case BsonType.INT64:
                return 8;
            case BsonType.DECIMAL128:
                return 16;
            case BsonType.MIN_KEY:
            case BsonType.MAX_KEY:
                return 0;
            default:
                throw new IllegalArgumentException("Unsupported BSON type: 0x" + Integer.toHexString(type & 0xFF));
        }
    }

    private void ensureCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = new Object[elements.length];
                }
            }
        }
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public boolean isEmpty() {
        return elements.length == 0;
    }

    @Override
    public byte getType(int index) {
        if (index < 0 || index >= elements.length) {
            return 0;
        }
        return elements[index].type;
    }

    @Override
    public int getInt32(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.INT32) {
            throw new IllegalArgumentException("Element at index " + index + " is not INT32");
        }

        // Check cache
        if (cache != null && cache[index] != null) {
            return (Integer) cache[index];
        }

        int value = Int32Parser.readDirect(data, element.valueOffset);

        ensureCache();
        cache[index] = value;

        return value;
    }

    @Override
    public int getInt32(int index, int defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.INT32) {
            return defaultValue;
        }
        return getInt32(index);
    }

    @Override
    public long getInt64(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.INT64) {
            throw new IllegalArgumentException("Element at index " + index + " is not INT64");
        }

        if (cache != null && cache[index] != null) {
            return (Long) cache[index];
        }

        long value = Int64Parser.readDirect(data, element.valueOffset);

        ensureCache();
        cache[index] = value;

        return value;
    }

    @Override
    public long getInt64(int index, long defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.INT64) {
            return defaultValue;
        }
        return getInt64(index);
    }

    @Override
    public double getDouble(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.DOUBLE) {
            throw new IllegalArgumentException("Element at index " + index + " is not DOUBLE");
        }

        if (cache != null && cache[index] != null) {
            return (Double) cache[index];
        }

        double value = DoubleParser.readDirect(data, element.valueOffset);

        ensureCache();
        cache[index] = value;

        return value;
    }

    @Override
    public double getDouble(int index, double defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.DOUBLE) {
            return defaultValue;
        }
        return getDouble(index);
    }

    @Override
    public boolean getBoolean(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.BOOLEAN) {
            throw new IllegalArgumentException("Element at index " + index + " is not BOOLEAN");
        }

        if (cache != null && cache[index] != null) {
            return (Boolean) cache[index];
        }

        boolean value = BooleanParser.readDirect(data, element.valueOffset);

        ensureCache();
        cache[index] = value;

        return value;
    }

    @Override
    public boolean getBoolean(int index, boolean defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.BOOLEAN) {
            return defaultValue;
        }
        return getBoolean(index);
    }

    @Override
    public String getString(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.STRING && element.type != BsonType.JAVASCRIPT && element.type != BsonType.SYMBOL) {
            throw new IllegalArgumentException("Element at index " + index + " is not STRING");
        }

        if (cache != null && cache[index] != null) {
            return (String) cache[index];
        }

        String value = StringParser.readDirect(data, element.valueOffset);

        ensureCache();
        cache[index] = value;

        return value;
    }

    @Override
    public String getString(int index, String defaultValue) {
        if (index < 0 || index >= elements.length) {
            return defaultValue;
        }
        byte type = elements[index].type;
        if (type != BsonType.STRING && type != BsonType.JAVASCRIPT && type != BsonType.SYMBOL) {
            return defaultValue;
        }
        return getString(index);
    }

    @Override
    public BsonDocument getDocument(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.DOCUMENT) {
            throw new IllegalArgumentException("Element at index " + index + " is not DOCUMENT");
        }

        if (cache != null && cache[index] != null) {
            return (BsonDocument) cache[index];
        }

        // Create child document (zero-copy)
        int docLength = Int32Parser.readDirect(data, element.valueOffset);
        IndexedBsonDocument childDoc = IndexedBsonDocument.parse(data, element.valueOffset, docLength);

        ensureCache();
        cache[index] = childDoc;

        return childDoc;
    }

    public BsonDocument getDocument(int index, BsonDocument defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.DOCUMENT) {
            return defaultValue;
        }
        return getDocument(index);
    }

    @Override
    public BsonArray getArray(int index) {
        if (index < 0 || index >= elements.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + elements.length);
        }

        ElementIndex element = elements[index];
        if (element.type != BsonType.ARRAY) {
            throw new IllegalArgumentException("Element at index " + index + " is not ARRAY");
        }

        if (cache != null && cache[index] != null) {
            return (BsonArray) cache[index];
        }

        // Create child array (zero-copy, recursive)
        int arrayLength = Int32Parser.readDirect(data, element.valueOffset);
        IndexedBsonArray childArray = IndexedBsonArray.parse(data, element.valueOffset, arrayLength);

        ensureCache();
        cache[index] = childArray;

        return childArray;
    }

    public BsonArray getArray(int index, BsonArray defaultValue) {
        if (index < 0 || index >= elements.length || elements[index].type != BsonType.ARRAY) {
            return defaultValue;
        }
        return getArray(index);
    }

    @Override
    public Object get(int index) {
        if (index < 0 || index >= elements.length) {
            return null;
        }

        // Check cache first
        if (cache != null && cache[index] != null) {
            return cache[index];
        }

        ElementIndex element = elements[index];
        Object value;

        switch (element.type) {
            case BsonType.INT32:
                value = Int32Parser.readDirect(data, element.valueOffset);
                break;
            case BsonType.INT64:
                value = Int64Parser.readDirect(data, element.valueOffset);
                break;
            case BsonType.DOUBLE:
                value = DoubleParser.readDirect(data, element.valueOffset);
                break;
            case BsonType.BOOLEAN:
                value = BooleanParser.readDirect(data, element.valueOffset);
                break;
            case BsonType.STRING:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
                value = StringParser.readDirect(data, element.valueOffset);
                break;
            case BsonType.DOCUMENT:
                int docLength = Int32Parser.readDirect(data, element.valueOffset);
                value = IndexedBsonDocument.parse(data, element.valueOffset, docLength);
                break;
            case BsonType.ARRAY:
                int arrayLength = Int32Parser.readDirect(data, element.valueOffset);
                value = IndexedBsonArray.parse(data, element.valueOffset, arrayLength);
                break;
            case BsonType.DATE_TIME:
                value = DateTimeParser.readDirect(data, element.valueOffset);
                break;
            case BsonType.NULL:
            case BsonType.UNDEFINED:
                value = null;
                break;
            default:
                throw new UnsupportedOperationException("Type not yet supported: 0x" + Integer.toHexString(element.type & 0xFF));
        }

        ensureCache();
        cache[index] = value;

        return value;
    }


    @Override
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < elements.length; i++) {
            if (i > 0) sb.append(",");

            Object value = get(i);
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof BsonDocument) {
                sb.append(((BsonDocument) value).toJson());
            } else if (value instanceof BsonArray) {
                sb.append(((BsonArray) value).toJson());
            } else {
                sb.append(value);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < elements.length;
            }

            @Override
            public Object next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                return get(currentIndex++);
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("IndexedBsonArray{size=").append(elements.length);
        sb.append(", cached=").append(cache != null ? countCached() : 0);
        sb.append("}");
        return sb.toString();
    }

    private int countCached() {
        if (cache == null) return 0;
        int count = 0;
        for (Object o : cache) {
            if (o != null) count++;
        }
        return count;
    }
}
