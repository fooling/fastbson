package com.cloud.fastbson.processor;

import com.cloud.fastbson.annotation.BranchOrder;
import com.cloud.fastbson.util.BsonType;
import org.junit.jupiter.api.Test;

import javax.lang.model.SourceVersion;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BranchOrderProcessor}.
 *
 * <p>Tests the processor's configuration and internal logic.
 * APT generation is tested through actual usage in user projects.
 */
class BranchOrderProcessorTest {

    @Test
    void testProcessor_SupportsSourceVersion8() {
        BranchOrderProcessor processor = new BranchOrderProcessor();
        assertEquals(SourceVersion.RELEASE_8, processor.getSupportedSourceVersion());
    }

    @Test
    void testProcessor_SupportsBranchOrderAnnotation() {
        BranchOrderProcessor processor = new BranchOrderProcessor();
        Set<String> supported = processor.getSupportedAnnotationTypes();
        assertTrue(supported.contains("com.cloud.fastbson.annotation.BranchOrder"));
    }

    @Test
    void testProcessor_HasCorrectAnnotation() {
        // Verify @SupportedAnnotationTypes is present
        assertNotNull(BranchOrderProcessor.class.getAnnotation(
            javax.annotation.processing.SupportedAnnotationTypes.class));
    }

    @Test
    void testProcessor_HasCorrectSourceVersion() {
        // Verify @SupportedSourceVersion is present
        assertNotNull(BranchOrderProcessor.class.getAnnotation(
            javax.annotation.processing.SupportedSourceVersion.class));
    }

    @Test
    void testProcessor_ExtendsAbstractProcessor() {
        BranchOrderProcessor processor = new BranchOrderProcessor();
        assertTrue(processor instanceof javax.annotation.processing.AbstractProcessor);
    }

    @Test
    void testBranchOrderAnnotation_HasValueAttribute() throws NoSuchMethodException {
        assertNotNull(BranchOrder.class.getMethod("value"));
    }

    @Test
    void testBranchOrderAnnotation_HasWorkloadAttribute() throws NoSuchMethodException {
        assertNotNull(BranchOrder.class.getMethod("workload"));
    }

    @Test
    void testBranchOrderAnnotation_WorkloadDefaultsToEmpty() throws NoSuchMethodException {
        String defaultValue = (String) BranchOrder.class.getMethod("workload").getDefaultValue();
        assertEquals("", defaultValue);
    }

    @Test
    void testBranchOrderAnnotation_IsRuntimeRetention() {
        java.lang.annotation.Retention retention =
            BranchOrder.class.getAnnotation(java.lang.annotation.Retention.class);
        assertNotNull(retention);
        assertEquals(java.lang.annotation.RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    void testBranchOrderAnnotation_TargetsType() {
        java.lang.annotation.Target target =
            BranchOrder.class.getAnnotation(java.lang.annotation.Target.class);
        assertNotNull(target);
        assertEquals(1, target.value().length);
        assertEquals(java.lang.annotation.ElementType.TYPE, target.value()[0]);
    }

    // Test class with @BranchOrder for verification
    @BranchOrder(value = {BsonType.INT64, BsonType.DOUBLE}, workload = "test")
    static class TestDispatcher {}

    @Test
    void testBranchOrderAnnotation_CanBeApplied() {
        BranchOrder annotation = TestDispatcher.class.getAnnotation(BranchOrder.class);
        assertNotNull(annotation);
    }

    @Test
    void testBranchOrderAnnotation_ValueIsReadable() {
        BranchOrder annotation = TestDispatcher.class.getAnnotation(BranchOrder.class);
        byte[] order = annotation.value();
        assertEquals(2, order.length);
        assertEquals(BsonType.INT64, order[0]);
        assertEquals(BsonType.DOUBLE, order[1]);
    }

    @Test
    void testBranchOrderAnnotation_WorkloadIsReadable() {
        BranchOrder annotation = TestDispatcher.class.getAnnotation(BranchOrder.class);
        assertEquals("test", annotation.workload());
    }

    @BranchOrder({BsonType.STRING})
    static class MinimalDispatcher {}

    @Test
    void testBranchOrderAnnotation_WorkloadCanBeOmitted() {
        BranchOrder annotation = MinimalDispatcher.class.getAnnotation(BranchOrder.class);
        assertNotNull(annotation);
        assertEquals("", annotation.workload());
    }
}
