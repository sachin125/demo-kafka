package com.example.avro.mapper;



import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.lang.reflect.Field;


public interface AvroBaseMapper<Entity, Avro> {
    Avro toAvro(Entity entity);
    Entity toEntity(Avro avro);


    default GenericRecord toAvro(Object entity, String entityType) {
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
            throw new RuntimeException("Failed to convert entity to Avro: " + entityType, e);
        }
    }
}
