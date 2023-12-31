package org.opensearch.dataprepper.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.dataprepper.model.sink.OutputCodecContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventDefinedAvroEventConverterTest {
    @Mock
    private SchemaChooser schemaChooser;
    @Mock(lenient = true)
    private Schema schema;
    private OutputCodecContext codecContext = null;

    @BeforeEach
    void setUp() {
        when(schema.getType()).thenReturn(Schema.Type.RECORD);
    }

    private EventDefinedAvroEventConverter createObjectUnderTest() {
        return new EventDefinedAvroEventConverter(schemaChooser);
    }

    @ParameterizedTest
    @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
    void convertEventDataToAvro_does_not_need_to_getField_on_empty_map() {
        Map<String, Object> data = Collections.emptyMap();
        GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

        assertThat(actualRecord, notNullValue());
        assertThat(actualRecord.getSchema(), equalTo(schema));

        verify(schema, never()).getField(anyString());
    }

    @Nested
    class WithPrimitiveField {

        private String fieldName;
        @Mock(lenient = true)
        private Schema.Field field;
        @Mock
        private Schema fieldSchema;

        @BeforeEach
        void setUp() {
            fieldName = UUID.randomUUID().toString();
            when(schema.getField(fieldName)).thenReturn(field);
            when(schema.getFields()).thenReturn(Collections.singletonList(field));
            when(field.name()).thenReturn(fieldName);
            when(field.schema()).thenReturn(fieldSchema);
            when(field.pos()).thenReturn(0);
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_adds_fields(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(fieldName, value);
            when(fieldSchema.getType()).thenReturn(expectedType);

            when(schemaChooser.chooseSchema(fieldSchema)).thenReturn(fieldSchema);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(fieldName), notNullValue());
            assertThat(actualRecord.get(fieldName), instanceOf(value.getClass()));
            assertThat(actualRecord.get(fieldName), equalTo(value));
        }


        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_non_present_fields(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.emptyMap();

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(fieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_null_values(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.singletonMap(fieldName, null);
            when(fieldSchema.getType()).thenReturn(expectedType);

            when(schemaChooser.chooseSchema(fieldSchema)).thenReturn(fieldSchema);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(fieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_fields_if_should_not_include_when_not_in_schema(Object value, Schema.Type expectedType) {
            when(schemaChooser.chooseSchema(fieldSchema)).thenReturn(fieldSchema);

            String nonFieldKey = randomAvroName();
            Map<String, Object> data = Map.of(
                    fieldName, value,
                    nonFieldKey, value
            );

            codecContext = mock(OutputCodecContext.class);
            when(codecContext.shouldNotIncludeKey(fieldName)).thenReturn(false);
            when(codecContext.shouldNotIncludeKey(nonFieldKey)).thenReturn(true);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(fieldName), notNullValue());
            assertThat(actualRecord.get(fieldName), instanceOf(value.getClass()));
            assertThat(actualRecord.get(fieldName), equalTo(value));
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_fields_if_should_not_include_when_in_schema(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(fieldName, value);

            codecContext = mock(OutputCodecContext.class);
            when(codecContext.shouldNotIncludeKey(fieldName)).thenReturn(true);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(fieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_throws_on_fields_which_are_not_defined_in_the_schema(Object value, Schema.Type expectedType) {
            lenient().when(schemaChooser.chooseSchema(fieldSchema)).thenReturn(fieldSchema);

            String nonFieldKey = randomAvroName();
            Map<String, Object> data = Map.of(
                    fieldName, value,
                    nonFieldKey, value
                    );

            codecContext = mock(OutputCodecContext.class);

            EventDefinedAvroEventConverter objectUnderTest = createObjectUnderTest();
            RuntimeException actualException = assertThrows(RuntimeException.class, () -> objectUnderTest.convertEventDataToAvro(schema, data, codecContext));

            assertThat(actualException.getMessage(), notNullValue());
            assertThat(actualException.getMessage(), containsString(nonFieldKey));
        }
    }

    @Nested
    class WithRecordField {
        private String recordFieldName;
        private String nestedFieldName;
        @Mock(lenient = true)
        private Schema.Field nestedField;
        @Mock
        private Schema nestedFieldSchema;
        @Mock
        private Schema recordFieldSchema;

        @BeforeEach
        void setUp() {
            recordFieldName = randomAvroName();
            nestedFieldName = randomAvroName();

            when(nestedField.name()).thenReturn(nestedFieldName);
            when(nestedField.schema()).thenReturn(nestedFieldSchema);
            when(nestedField.pos()).thenReturn(0);

            lenient().when(recordFieldSchema.getType()).thenReturn(Schema.Type.RECORD);
            lenient().when(recordFieldSchema.getField(nestedFieldName)).thenReturn(nestedField);
            lenient().when(recordFieldSchema.getFields()).thenReturn(Collections.singletonList(nestedField));

            Schema.Field recordField = mock(Schema.Field.class);
            when(schema.getField(recordFieldName)).thenReturn(recordField);
            when(schema.getFields()).thenReturn(Collections.singletonList(recordField));

            lenient().when(recordField.schema()).thenReturn(recordFieldSchema);
            lenient().when(recordField.pos()).thenReturn(0);

            lenient().when(schemaChooser.chooseSchema(recordFieldSchema)).thenReturn(recordFieldSchema);
            lenient().when(schemaChooser.chooseSchema(nestedFieldSchema)).thenReturn(nestedFieldSchema);
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_adds_nested_fields(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(recordFieldName, Map.of(nestedFieldName, value));
            when(nestedFieldSchema.getType()).thenReturn(expectedType);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), notNullValue());
            assertThat(actualRecord.get(recordFieldName), instanceOf(GenericData.Record.class));
            GenericData.Record actualSubRecord = (GenericData.Record) actualRecord.get(recordFieldName);

            assertThat(actualSubRecord.getSchema(), notNullValue());
            assertThat(actualSubRecord.getSchema(), equalTo(recordFieldSchema));
            assertThat(actualSubRecord.getSchema().getType(), equalTo(Schema.Type.RECORD));
            assertThat(actualSubRecord.get(nestedFieldName), notNullValue());
            assertThat(actualSubRecord.get(nestedFieldName), instanceOf(value.getClass()));
            assertThat(actualSubRecord.get(nestedFieldName), equalTo(value));
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_non_present_records(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.emptyMap();

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_null_record(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.singletonMap(recordFieldName, null);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_non_present_nested_fields(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(recordFieldName, Collections.emptyMap());

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), notNullValue());
            assertThat(actualRecord.get(recordFieldName), instanceOf(GenericData.Record.class));
            GenericData.Record actualSubRecord = (GenericData.Record) actualRecord.get(recordFieldName);

            assertThat(actualSubRecord.getSchema(), notNullValue());
            assertThat(actualSubRecord.getSchema(), equalTo(recordFieldSchema));
            assertThat(actualSubRecord.getSchema().getType(), equalTo(Schema.Type.RECORD));
            assertThat(actualSubRecord.get(nestedFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_null_nested_fields(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(recordFieldName, Collections.singletonMap(nestedFieldName, null));
            when(nestedFieldSchema.getType()).thenReturn(expectedType);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), notNullValue());
            assertThat(actualRecord.get(recordFieldName), instanceOf(GenericData.Record.class));
            GenericData.Record actualSubRecord = (GenericData.Record) actualRecord.get(recordFieldName);

            assertThat(actualSubRecord.getSchema(), notNullValue());
            assertThat(actualSubRecord.getSchema(), equalTo(recordFieldSchema));
            assertThat(actualSubRecord.getSchema().getType(), equalTo(Schema.Type.RECORD));
            assertThat(actualSubRecord.get(nestedFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_does_not_skip_nested_fields_because_include_keys_are_not_paths(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(recordFieldName, Map.of(nestedFieldName, value));
            when(nestedFieldSchema.getType()).thenReturn(expectedType);

            codecContext = mock(OutputCodecContext.class);
            lenient().when(codecContext.shouldNotIncludeKey(nestedFieldName)).thenReturn(true);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(recordFieldName), notNullValue());
            assertThat(actualRecord.get(recordFieldName), instanceOf(GenericData.Record.class));
            GenericData.Record actualSubRecord = (GenericData.Record) actualRecord.get(recordFieldName);

            assertThat(actualSubRecord.getSchema(), notNullValue());
            assertThat(actualSubRecord.getSchema(), equalTo(recordFieldSchema));
            assertThat(actualSubRecord.getSchema().getType(), equalTo(Schema.Type.RECORD));
            assertThat(actualSubRecord.get(nestedFieldName), notNullValue());
            assertThat(actualSubRecord.get(nestedFieldName), instanceOf(value.getClass()));
            assertThat(actualSubRecord.get(nestedFieldName), equalTo(value));
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_throws_on_fields_which_are_not_defined_in_the_schema(Object value, Schema.Type expectedType) {
            String nonFieldKey = UUID.randomUUID().toString();
            Map<String, Object> data = Map.of(
                    recordFieldName,
                    Map.of(
                            nestedFieldName, value,
                            nonFieldKey, value
                    ));

            codecContext = mock(OutputCodecContext.class);
            when(codecContext.shouldNotIncludeKey(nonFieldKey)).thenReturn(true);

            EventDefinedAvroEventConverter objectUnderTest = createObjectUnderTest();
            RuntimeException actualException = assertThrows(RuntimeException.class, () -> objectUnderTest.convertEventDataToAvro(schema, data, codecContext));

            assertThat(actualException.getMessage(), notNullValue());
            assertThat(actualException.getMessage(), containsString(nonFieldKey));
        }
    }

    @Nested
    class WithArrayField {
        private String arrayFieldName;
        private String nestedFieldName;
        @Mock(lenient = true)
        private Schema.Field nestedField;
        @Mock
        private Schema nestedFieldSchema;
        @Mock
        private Schema arrayFieldSchema;

        @BeforeEach
        void setUp() {
            arrayFieldName = randomAvroName();
            nestedFieldName = randomAvroName();

            when(nestedField.name()).thenReturn(nestedFieldName);
            when(nestedField.schema()).thenReturn(nestedFieldSchema);
            when(nestedField.pos()).thenReturn(0);

            lenient().when(arrayFieldSchema.getType()).thenReturn(Schema.Type.ARRAY);

            Schema.Field recordField = mock(Schema.Field.class);
            when(schema.getField(arrayFieldName)).thenReturn(recordField);
            when(schema.getFields()).thenReturn(Collections.singletonList(recordField));

            lenient().when(recordField.schema()).thenReturn(arrayFieldSchema);
            when(recordField.pos()).thenReturn(0);

            lenient().when(schemaChooser.chooseSchema(arrayFieldSchema)).thenReturn(arrayFieldSchema);
            lenient().when(schemaChooser.chooseSchema(nestedFieldSchema)).thenReturn(nestedFieldSchema);
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_adds_array_values(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(arrayFieldName, List.of(value));

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(arrayFieldName), notNullValue());
            assertThat(actualRecord.get(arrayFieldName), instanceOf(GenericData.Array.class));
            GenericData.Array actualArray = (GenericData.Array) actualRecord.get(arrayFieldName);

            assertThat(actualArray.getSchema(), notNullValue());
            assertThat(actualArray.getSchema(), equalTo(arrayFieldSchema));
            assertThat(actualArray.getSchema().getType(), equalTo(Schema.Type.ARRAY));
            assertThat(actualArray.size(), equalTo(1));
            assertThat(actualArray.get(0), notNullValue());
            assertThat(actualArray.get(0), equalTo(value));
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_non_present_arrays(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.emptyMap();

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(arrayFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_skips_null_array(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Collections.singletonMap(arrayFieldName, null);

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(arrayFieldName), nullValue());
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_includes_empty_arrays(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(arrayFieldName, Collections.emptyList());

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(arrayFieldName), notNullValue());
            assertThat(actualRecord.get(arrayFieldName), instanceOf(GenericData.Array.class));
            GenericData.Array actualArray = (GenericData.Array) actualRecord.get(arrayFieldName);

            assertThat(actualArray.getSchema(), notNullValue());
            assertThat(actualArray.getSchema(), equalTo(arrayFieldSchema));
            assertThat(actualArray.getSchema().getType(), equalTo(Schema.Type.ARRAY));
            assertThat(actualArray.size(), equalTo(0));
        }

        @ParameterizedTest
        @ArgumentsSource(PrimitiveClassesToTypesArgumentsProvider.class)
        void convertEventDataToAvro_includes_null_array_elements(Object value, Schema.Type expectedType) {
            Map<String, Object> data = Map.of(arrayFieldName, Collections.singletonList(null));

            GenericRecord actualRecord = createObjectUnderTest().convertEventDataToAvro(schema, data, codecContext);

            assertThat(actualRecord, notNullValue());
            assertThat(actualRecord.getSchema(), equalTo(schema));

            assertThat(actualRecord.get(arrayFieldName), notNullValue());
            assertThat(actualRecord.get(arrayFieldName), instanceOf(GenericData.Array.class));
            GenericData.Array actualArray = (GenericData.Array) actualRecord.get(arrayFieldName);

            assertThat(actualArray.getSchema(), notNullValue());
            assertThat(actualArray.getSchema(), equalTo(arrayFieldSchema));
            assertThat(actualArray.getSchema().getType(), equalTo(Schema.Type.ARRAY));
            assertThat(actualArray.size(), equalTo(1));
            assertThat(actualArray.get(0), nullValue());
        }
    }

    private static String randomAvroName() {
        return "a" + UUID.randomUUID().toString().replaceAll("-", "");
    }
}