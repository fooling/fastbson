package com.cloud.fastbson.compatibility;

import java.util.Arrays;

/**
 * BSON test case containing raw BSON data and expected values.
 * Used for cross-implementation compatibility testing.
 */
public class BsonTestCase {
    private final String name;
    private final String description;
    private final byte[] bsonData;
    private final TestExpectation[] expectations;
    private final boolean toJsonSupported;

    public BsonTestCase(String name, String description, byte[] bsonData, TestExpectation... expectations) {
        this(name, description, bsonData, true, expectations);
    }

    public BsonTestCase(String name, String description, byte[] bsonData, boolean toJsonSupported, TestExpectation... expectations) {
        this.name = name;
        this.description = description;
        this.bsonData = bsonData;
        this.toJsonSupported = toJsonSupported;
        this.expectations = expectations;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public byte[] getBsonData() {
        return bsonData;
    }

    public TestExpectation[] getExpectations() {
        return expectations;
    }

    public boolean isToJsonSupported() {
        return toJsonSupported;
    }

    @Override
    public String toString() {
        return "BsonTestCase{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", bsonDataLength=" + bsonData.length +
                ", expectations=" + Arrays.toString(expectations) +
                '}';
    }

    /**
     * Expected value for a specific field in the test case.
     */
    public static class TestExpectation {
        private final String fieldPath;
        private final Object expectedValue;
        private final Class<?> expectedType;

        public TestExpectation(String fieldPath, Object expectedValue, Class<?> expectedType) {
            this.fieldPath = fieldPath;
            this.expectedValue = expectedValue;
            this.expectedType = expectedType;
        }

        public String getFieldPath() {
            return fieldPath;
        }

        public Object getExpectedValue() {
            return expectedValue;
        }

        public Class<?> getExpectedType() {
            return expectedType;
        }

        @Override
        public String toString() {
            return "TestExpectation{" +
                    "fieldPath='" + fieldPath + '\'' +
                    ", expectedValue=" + expectedValue +
                    ", expectedType=" + expectedType.getSimpleName() +
                    '}';
        }
    }
}
