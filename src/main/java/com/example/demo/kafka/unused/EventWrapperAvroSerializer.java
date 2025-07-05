package com.example.demo.kafka.unused;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Slf4j
@Component
public class EventWrapperAvroSerializer {


    public SpecificRecord convertToAvroAndGetSpecificRecord(Object jpaEntity, String entityType) {
        try {
            // Load Avro schema
            String schemaPath = "classpath:avro/" + entityType + ".avsc";
            Resource resource = new PathMatchingResourcePatternResolver().getResource(schemaPath);
            Schema schema = new Schema.Parser().parse(resource.getInputStream());

            // Instantiate Avro class (e.g., com.example.kafka.model.User)
            String avroClassName = schema.getNamespace() + "." + schema.getName();
            Class<?> avroClass = Class.forName(avroClassName);
            SpecificRecord avroRecord = (SpecificRecord) avroClass.getDeclaredConstructor().newInstance();

            // Map JPA entity fields to Avro record
            for (Field jpaField : jpaEntity.getClass().getDeclaredFields()) {
                jpaField.setAccessible(true);
                String fieldName = jpaField.getName();
                if (schema.getField(fieldName) != null) {
                    Object value = jpaField.get(jpaEntity);
                    // Find setter method (e.g., setId)
                    String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    Method setter = avroClass.getMethod(setterName, jpaField.getType());
                    setter.invoke(avroRecord, value);
                }
            }
            return avroRecord;
        } catch (IOException | ReflectiveOperationException e) {
            log.error("Failed to convert entity to Avro: {}: {}", entityType, e.getMessage(), e);
            throw new RuntimeException("Failed to convert entity to Avro: " + entityType, e);
        }
    }

    public GenericRecord convertToAvroAndGetGenericRecord(Object entity, String entityType) {
        try {
            // Load Avro schema from resources/avro/<Entity>.avsc
            String schemaPath = "classpath:avro/" + entityType + ".avsc";
            Resource resource = new PathMatchingResourcePatternResolver().getResource(schemaPath);
            Schema schema = new Schema.Parser().parse(resource.getInputStream());

            GenericRecord record = new GenericData.Record(schema);
            // Reflectively map entity fields to Avro record
            for (Field field : entity.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldName = field.getName();
                if (schema.getField(fieldName) != null) {
                    record.put(fieldName, field.get(entity));
                }
            }
            return record;
        } catch (IOException | IllegalAccessException e) {
            log.error("Failed to convert entity to Avro: {}: {}", entityType, e.getMessage(), e);
            throw new RuntimeException("Failed to convert entity to Avro: " + entityType, e);
        }
    }
    
}
