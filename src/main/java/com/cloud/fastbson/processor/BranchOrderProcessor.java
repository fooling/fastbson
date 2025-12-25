package com.cloud.fastbson.processor;

import com.cloud.fastbson.annotation.BranchOrder;
import com.cloud.fastbson.util.BsonType;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Annotation processor that generates optimized TypeDispatcher implementations
 * based on {@link BranchOrder} annotations.
 *
 * <p>This processor reads classes annotated with {@code @BranchOrder} and generates
 * corresponding implementation classes with switch/if-else statements ordered
 * according to the specified type priority.
 *
 * <p><b>How it works:</b>
 * <ol>
 *   <li>Find all classes annotated with {@code @BranchOrder}</li>
 *   <li>Read the type priority order from the annotation</li>
 *   <li>Generate an implementation class with optimized dispatch logic</li>
 *   <li>The generated class implements {@link TypeDispatcher}</li>
 * </ol>
 *
 * <p><b>Generated Class Naming:</b>
 * <ul>
 *   <li>Input: {@code com.example.TimeSeriesDispatcher}</li>
 *   <li>Output: {@code com.example.TimeSeriesDispatcherImpl}</li>
 * </ul>
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * // User defines:
 * @BranchOrder(value = {BsonType.INT64, BsonType.DOUBLE}, workload = "timeseries")
 * public abstract class TimeSeriesDispatcher implements TypeDispatcher {}
 *
 * // Processor generates TimeSeriesDispatcherImpl with optimized dispatch
 * }</pre>
 *
 * @author FastBSON
 * @since Phase 3.5
 */
@SupportedAnnotationTypes("com.cloud.fastbson.annotation.BranchOrder")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BranchOrderProcessor extends AbstractProcessor {

    private Filer filer;
    private Messager messager;

    /**
     * All known BSON types for complete dispatch coverage.
     */
    private static final byte[] ALL_BSON_TYPES = {
        BsonType.INT32,      // Most common in general documents
        BsonType.STRING,
        BsonType.DOUBLE,
        BsonType.INT64,
        BsonType.BOOLEAN,
        BsonType.DOCUMENT,
        BsonType.ARRAY,
        BsonType.OBJECT_ID,
        BsonType.DATE_TIME,
        BsonType.NULL,
        BsonType.BINARY,
        BsonType.REGEX,
        BsonType.JAVASCRIPT,
        BsonType.SYMBOL,
        BsonType.JAVASCRIPT_WITH_SCOPE,
        BsonType.TIMESTAMP,
        BsonType.DECIMAL128,
        BsonType.MIN_KEY,
        BsonType.MAX_KEY
    };

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(BranchOrder.class)) {
            if (element.getKind() != ElementKind.CLASS
                && element.getKind() != ElementKind.INTERFACE
                && element.getKind() != ElementKind.ENUM) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@BranchOrder can only be applied to classes, interfaces, or enums", element);
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            try {
                generateDispatcher(typeElement);
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate dispatcher: " + e.getMessage(), element);
            }
        }
        return true;
    }

    /**
     * Generates a TypeDispatcher implementation for the annotated class.
     */
    private void generateDispatcher(TypeElement typeElement) throws IOException {
        BranchOrder annotation = typeElement.getAnnotation(BranchOrder.class);
        byte[] priorityOrder = annotation.value();
        String workload = annotation.workload();

        // Get package and class names
        PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement();
        String packageName = packageElement.getQualifiedName().toString();
        String originalClassName = typeElement.getSimpleName().toString();
        String generatedClassName = originalClassName + "Impl";

        // Build the complete type order: priority types first, then remaining types
        byte[] completeOrder = buildCompleteOrder(priorityOrder);

        // Generate the class
        TypeSpec dispatcherImpl = TypeSpec.classBuilder(generatedClassName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ClassName.get(TypeDispatcher.class))
            .addJavadoc("Generated TypeDispatcher implementation for {@link $L}.\n", originalClassName)
            .addJavadoc("\n")
            .addJavadoc("<p>Branch order optimized for: $L\n", workload.isEmpty() ? "custom workload" : workload)
            .addJavadoc("\n")
            .addJavadoc("<p>Priority types: $L\n", formatTypes(priorityOrder))
            .addJavadoc("\n")
            .addJavadoc("@generated by BranchOrderProcessor\n")
            .addField(buildBranchOrderField(priorityOrder))
            .addField(buildWorkloadField(workload))
            .addMethod(buildDispatchMethod(completeOrder))
            .addMethod(buildSkipMethod(completeOrder))
            .addMethod(buildGetBranchOrderMethod())
            .addMethod(buildGetWorkloadDescriptionMethod())
            .build();

        // Write the file
        JavaFile javaFile = JavaFile.builder(packageName, dispatcherImpl)
            .addFileComment("Generated by BranchOrderProcessor - DO NOT EDIT")
            .indent("    ")
            .build();

        javaFile.writeTo(filer);

        messager.printMessage(Diagnostic.Kind.NOTE,
            "Generated " + packageName + "." + generatedClassName +
            " with branch order: " + formatTypes(priorityOrder));
    }

    /**
     * Builds the complete type order: priority types first, then remaining types.
     */
    private byte[] buildCompleteOrder(byte[] priorityOrder) {
        Set<Byte> prioritySet = new HashSet<Byte>();
        for (byte type : priorityOrder) {
            prioritySet.add(type);
        }

        // Use LinkedHashSet to maintain order
        LinkedHashSet<Byte> completeOrderSet = new LinkedHashSet<Byte>();

        // Add priority types first
        for (byte type : priorityOrder) {
            completeOrderSet.add(type);
        }

        // Add remaining types
        for (byte type : ALL_BSON_TYPES) {
            if (!prioritySet.contains(type)) {
                completeOrderSet.add(type);
            }
        }

        byte[] result = new byte[completeOrderSet.size()];
        int i = 0;
        for (Byte type : completeOrderSet) {
            result[i++] = type;
        }
        return result;
    }

    /**
     * Builds the BRANCH_ORDER constant field.
     */
    private FieldSpec buildBranchOrderField(byte[] order) {
        StringBuilder initializer = new StringBuilder("new byte[] {");
        for (int i = 0; i < order.length; i++) {
            if (i > 0) {
                initializer.append(", ");
            }
            initializer.append(formatTypeByte(order[i]));
        }
        initializer.append("}");

        return FieldSpec.builder(ArrayTypeName.of(byte.class), "BRANCH_ORDER")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer(initializer.toString())
            .build();
    }

    /**
     * Builds the WORKLOAD_DESCRIPTION constant field.
     */
    private FieldSpec buildWorkloadField(String workload) {
        return FieldSpec.builder(String.class, "WORKLOAD_DESCRIPTION")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$S", workload)
            .build();
    }

    /**
     * Builds the dispatch method with optimized if-else chain.
     */
    private MethodSpec buildDispatchMethod(byte[] typeOrder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("dispatch")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(Object.class)
            .addParameter(ClassName.get("com.cloud.fastbson.reader", "BsonReader"), "reader")
            .addParameter(byte.class, "type");

        // Generate if-else chain in priority order
        for (int i = 0; i < typeOrder.length; i++) {
            byte bsonType = typeOrder[i];
            CodeBlock parseCode = getParseCode(bsonType);

            if (i == 0) {
                methodBuilder.beginControlFlow("if (type == $L)", formatTypeByte(bsonType));
            } else {
                methodBuilder.nextControlFlow("else if (type == $L)", formatTypeByte(bsonType));
            }

            methodBuilder.addCode(parseCode);
        }

        // Handle unknown types
        methodBuilder.nextControlFlow("else");
        methodBuilder.addStatement("return null");
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    /**
     * Builds the skip method with optimized if-else chain.
     */
    private MethodSpec buildSkipMethod(byte[] typeOrder) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("skip")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(int.class)
            .addParameter(ClassName.get("com.cloud.fastbson.reader", "BsonReader"), "reader")
            .addParameter(byte.class, "type");

        // Generate if-else chain in priority order
        for (int i = 0; i < typeOrder.length; i++) {
            byte bsonType = typeOrder[i];
            CodeBlock skipCode = getSkipCode(bsonType);

            if (i == 0) {
                methodBuilder.beginControlFlow("if (type == $L)", formatTypeByte(bsonType));
            } else {
                methodBuilder.nextControlFlow("else if (type == $L)", formatTypeByte(bsonType));
            }

            methodBuilder.addCode(skipCode);
        }

        // Handle unknown types
        methodBuilder.nextControlFlow("else");
        methodBuilder.addStatement("return 0");
        methodBuilder.endControlFlow();

        return methodBuilder.build();
    }

    /**
     * Builds the getBranchOrder method.
     */
    private MethodSpec buildGetBranchOrderMethod() {
        return MethodSpec.methodBuilder("getBranchOrder")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(ArrayTypeName.of(byte.class))
            .addStatement("return BRANCH_ORDER.clone()")
            .build();
    }

    /**
     * Builds the getWorkloadDescription method.
     */
    private MethodSpec buildGetWorkloadDescriptionMethod() {
        return MethodSpec.methodBuilder("getWorkloadDescription")
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return WORKLOAD_DESCRIPTION")
            .build();
    }

    /**
     * Gets the parse code for a specific BSON type.
     */
    private CodeBlock getParseCode(byte type) {
        CodeBlock.Builder builder = CodeBlock.builder();

        switch (type) {
            case BsonType.INT32:
                builder.addStatement("return $T.valueOf(reader.readInt32())", Integer.class);
                break;
            case BsonType.INT64:
                builder.addStatement("return $T.valueOf(reader.readInt64())", Long.class);
                break;
            case BsonType.DOUBLE:
                builder.addStatement("return $T.valueOf(reader.readDouble())", Double.class);
                break;
            case BsonType.STRING:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
                builder.addStatement("return reader.readString()");
                break;
            case BsonType.BOOLEAN:
                builder.addStatement("return $T.valueOf(reader.readByte() != 0)", Boolean.class);
                break;
            case BsonType.NULL:
                builder.addStatement("return null");
                break;
            case BsonType.DATE_TIME:
            case BsonType.TIMESTAMP:
                builder.addStatement("return $T.valueOf(reader.readInt64())", Long.class);
                break;
            case BsonType.OBJECT_ID:
                builder.addStatement("return $T.bytesToHex(reader.readBytes(12))",
                    ClassName.get("com.cloud.fastbson.util", "BsonUtils"));
                break;
            case BsonType.DOCUMENT:
                builder.addStatement("return $T.INSTANCE.parse(reader)",
                    ClassName.get("com.cloud.fastbson.handler.parsers", "DocumentParser"));
                break;
            case BsonType.ARRAY:
                builder.addStatement("return $T.INSTANCE.parse(reader)",
                    ClassName.get("com.cloud.fastbson.handler.parsers", "ArrayParser"));
                break;
            case BsonType.BINARY:
                builder.addStatement("int binLen = reader.readInt32()");
                builder.addStatement("byte subtype = reader.readByte()");
                builder.addStatement("byte[] data = reader.readBytes(binLen)");
                builder.addStatement("return data");
                break;
            case BsonType.REGEX:
                builder.addStatement("String pattern = reader.readCString()");
                builder.addStatement("String options = reader.readCString()");
                builder.addStatement("return pattern");
                break;
            case BsonType.JAVASCRIPT_WITH_SCOPE:
                builder.addStatement("int totalLen = reader.readInt32()");
                builder.addStatement("String code = reader.readString()");
                builder.addStatement("reader.skip(totalLen - 4 - 4 - code.length() - 1)");
                builder.addStatement("return code");
                break;
            case BsonType.DECIMAL128:
                builder.addStatement("reader.skip(16)");
                builder.addStatement("return null");
                break;
            case BsonType.MIN_KEY:
            case BsonType.MAX_KEY:
                builder.addStatement("return null");
                break;
            default:
                builder.addStatement("return null");
                break;
        }

        return builder.build();
    }

    /**
     * Gets the skip code for a specific BSON type.
     */
    private CodeBlock getSkipCode(byte type) {
        CodeBlock.Builder builder = CodeBlock.builder();

        switch (type) {
            case BsonType.INT32:
                builder.addStatement("reader.skip(4)");
                builder.addStatement("return 4");
                break;
            case BsonType.INT64:
            case BsonType.DOUBLE:
            case BsonType.DATE_TIME:
            case BsonType.TIMESTAMP:
                builder.addStatement("reader.skip(8)");
                builder.addStatement("return 8");
                break;
            case BsonType.BOOLEAN:
                builder.addStatement("reader.skip(1)");
                builder.addStatement("return 1");
                break;
            case BsonType.NULL:
            case BsonType.MIN_KEY:
            case BsonType.MAX_KEY:
                builder.addStatement("return 0");
                break;
            case BsonType.OBJECT_ID:
                builder.addStatement("reader.skip(12)");
                builder.addStatement("return 12");
                break;
            case BsonType.STRING:
            case BsonType.JAVASCRIPT:
            case BsonType.SYMBOL:
                builder.addStatement("int strLen = reader.readInt32()");
                builder.addStatement("reader.skip(strLen)");
                builder.addStatement("return 4 + strLen");
                break;
            case BsonType.DOCUMENT:
            case BsonType.ARRAY:
                builder.addStatement("int docLen = reader.readInt32()");
                builder.addStatement("reader.skip(docLen - 4)");
                builder.addStatement("return docLen");
                break;
            case BsonType.BINARY:
                builder.addStatement("int binLen = reader.readInt32()");
                builder.addStatement("reader.skip(1 + binLen)");
                builder.addStatement("return 4 + 1 + binLen");
                break;
            case BsonType.REGEX:
                builder.addStatement("reader.skipCString()");
                builder.addStatement("reader.skipCString()");
                builder.addStatement("return -1"); // Variable length
                break;
            case BsonType.JAVASCRIPT_WITH_SCOPE:
                builder.addStatement("int totalLen = reader.readInt32()");
                builder.addStatement("reader.skip(totalLen - 4)");
                builder.addStatement("return totalLen");
                break;
            case BsonType.DECIMAL128:
                builder.addStatement("reader.skip(16)");
                builder.addStatement("return 16");
                break;
            default:
                builder.addStatement("return 0");
                break;
        }

        return builder.build();
    }

    /**
     * Formats a byte constant as BsonType reference.
     */
    private String formatTypeByte(byte type) {
        return "com.cloud.fastbson.util.BsonType." + getTypeName(type);
    }

    /**
     * Gets the type name for a BSON type byte.
     */
    private String getTypeName(byte type) {
        switch (type) {
            case BsonType.DOUBLE: return "DOUBLE";
            case BsonType.STRING: return "STRING";
            case BsonType.DOCUMENT: return "DOCUMENT";
            case BsonType.ARRAY: return "ARRAY";
            case BsonType.BINARY: return "BINARY";
            case BsonType.OBJECT_ID: return "OBJECT_ID";
            case BsonType.BOOLEAN: return "BOOLEAN";
            case BsonType.DATE_TIME: return "DATE_TIME";
            case BsonType.NULL: return "NULL";
            case BsonType.REGEX: return "REGEX";
            case BsonType.JAVASCRIPT: return "JAVASCRIPT";
            case BsonType.SYMBOL: return "SYMBOL";
            case BsonType.JAVASCRIPT_WITH_SCOPE: return "JAVASCRIPT_WITH_SCOPE";
            case BsonType.INT32: return "INT32";
            case BsonType.TIMESTAMP: return "TIMESTAMP";
            case BsonType.INT64: return "INT64";
            case BsonType.DECIMAL128: return "DECIMAL128";
            case BsonType.MIN_KEY: return "MIN_KEY";
            case BsonType.MAX_KEY: return "MAX_KEY";
            default: return "UNKNOWN_" + String.format("0x%02X", type);
        }
    }

    /**
     * Formats type array as human-readable string.
     */
    private String formatTypes(byte[] types) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(" → ");
            }
            sb.append(getTypeName(types[i]));
        }
        return sb.toString();
    }
}
