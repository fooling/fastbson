package com.cloud.fastbson.processor;

import com.cloud.fastbson.reader.BsonReader;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TypeDispatcher} interface.
 */
class TypeDispatcherTest {

    /**
     * Simple mock implementation for testing the interface contract.
     */
    private static class MockTypeDispatcher implements TypeDispatcher {
        private final byte[] branchOrder;
        private final String workload;

        MockTypeDispatcher(byte[] branchOrder, String workload) {
            this.branchOrder = branchOrder;
            this.workload = workload;
        }

        @Override
        public Object dispatch(BsonReader reader, byte type) {
            if (type == BsonType.INT32) {
                return reader.readInt32();
            }
            if (type == BsonType.STRING) {
                return reader.readString();
            }
            return null;
        }

        @Override
        public int skip(BsonReader reader, byte type) {
            if (type == BsonType.INT32) {
                reader.skip(4);
                return 4;
            }
            if (type == BsonType.STRING) {
                int len = reader.readInt32();
                reader.skip(len);
                return 4 + len;
            }
            return 0;
        }

        @Override
        public byte[] getBranchOrder() {
            return branchOrder.clone();
        }

        @Override
        public String getWorkloadDescription() {
            return workload;
        }
    }

    @Test
    void testDispatch_Int32() {
        // Create BSON data for INT32: 42
        byte[] data = new byte[] {42, 0, 0, 0};  // Little-endian 42
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            "test"
        );

        Object result = dispatcher.dispatch(reader, BsonType.INT32);
        assertEquals(42, result);
    }

    @Test
    void testDispatch_String() {
        // Create BSON string: "hello" (length 6 including null)
        byte[] data = new byte[] {
            6, 0, 0, 0,  // length = 6 (includes null terminator)
            'h', 'e', 'l', 'l', 'o', 0  // "hello" + null
        };
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.STRING},
            "test"
        );

        Object result = dispatcher.dispatch(reader, BsonType.STRING);
        assertEquals("hello", result);
    }

    @Test
    void testDispatch_UnsupportedType_ReturnsNull() {
        byte[] data = new byte[16];
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            "test"
        );

        // DOCUMENT type is not handled by our mock
        Object result = dispatcher.dispatch(reader, BsonType.DOCUMENT);
        assertNull(result);
    }

    @Test
    void testSkip_Int32() {
        byte[] data = new byte[] {42, 0, 0, 0};
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            "test"
        );

        int skipped = dispatcher.skip(reader, BsonType.INT32);
        assertEquals(4, skipped);
        assertEquals(4, reader.position());
    }

    @Test
    void testSkip_String() {
        byte[] data = new byte[] {
            6, 0, 0, 0,  // length = 6
            'h', 'e', 'l', 'l', 'o', 0
        };
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.STRING},
            "test"
        );

        int skipped = dispatcher.skip(reader, BsonType.STRING);
        assertEquals(10, skipped);  // 4 (length) + 6 (content)
        assertEquals(10, reader.position());
    }

    @Test
    void testSkip_UnsupportedType_ReturnsZero() {
        byte[] data = new byte[16];
        BsonReader reader = new BsonReader(data);

        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            "test"
        );

        int skipped = dispatcher.skip(reader, BsonType.DOCUMENT);
        assertEquals(0, skipped);
    }

    @Test
    void testGetBranchOrder() {
        byte[] order = {BsonType.INT64, BsonType.DOUBLE, BsonType.STRING};
        TypeDispatcher dispatcher = new MockTypeDispatcher(order, "timeseries");

        byte[] result = dispatcher.getBranchOrder();
        assertArrayEquals(order, result);
    }

    @Test
    void testGetBranchOrder_ReturnsCopy() {
        byte[] order = {BsonType.INT64, BsonType.DOUBLE};
        TypeDispatcher dispatcher = new MockTypeDispatcher(order, "test");

        byte[] result1 = dispatcher.getBranchOrder();
        byte[] result2 = dispatcher.getBranchOrder();

        // Should return copies, not the same array reference
        assertNotSame(result1, result2);
        assertArrayEquals(result1, result2);
    }

    @Test
    void testGetWorkloadDescription() {
        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            "Web API documents"
        );

        assertEquals("Web API documents", dispatcher.getWorkloadDescription());
    }

    @Test
    void testGetWorkloadDescription_Empty() {
        TypeDispatcher dispatcher = new MockTypeDispatcher(
            new byte[] {BsonType.INT32},
            ""
        );

        assertEquals("", dispatcher.getWorkloadDescription());
    }

    @Test
    void testInterface_HasRequiredMethods() {
        // Verify the interface declares all required methods
        Class<TypeDispatcher> clazz = TypeDispatcher.class;

        assertDoesNotThrow(() -> clazz.getMethod("dispatch", BsonReader.class, byte.class));
        assertDoesNotThrow(() -> clazz.getMethod("skip", BsonReader.class, byte.class));
        assertDoesNotThrow(() -> clazz.getMethod("getBranchOrder"));
        assertDoesNotThrow(() -> clazz.getMethod("getWorkloadDescription"));
    }

    @Test
    void testInterface_IsInterface() {
        assertTrue(TypeDispatcher.class.isInterface());
    }
}
