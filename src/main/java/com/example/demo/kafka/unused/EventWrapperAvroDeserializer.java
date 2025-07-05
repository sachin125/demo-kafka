package com.example.demo.kafka.unused;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.extern.slf4j.Slf4j;

import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Deserializer;

import com.example.demo.kafka.factory.EventWrapper;

import java.util.Map;

@Slf4j
public class EventWrapperAvroDeserializer implements Deserializer<EventWrapper<? extends SpecificRecord>> {

    private final KafkaAvroDeserializer innerDeserializer = new KafkaAvroDeserializer();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        innerDeserializer.configure(configs, isKey);
    }

    @Override
    public EventWrapper<? extends SpecificRecord> deserialize(String topic, byte[] data) {
        try {
            Object result = innerDeserializer.deserialize(topic, data);

            if (!(result instanceof GenericRecord generic)) {
                log.error("Expected GenericRecord, got: {} from topic {}", result, topic);
                throw new IllegalArgumentException("Expected GenericRecord, got: " + result);
            }

            // Extract EventWrapper fields using correct field names
            String eventId = generic.get("eventId") != null ? generic.get("eventId").toString() : null;
            String eventType = generic.get("eventType") != null ? generic.get("eventType").toString() : null;
            String version = generic.get("version") != null ? generic.get("version").toString() : null;
            String topicName = generic.get("topic") != null ? generic.get("topic").toString() : null;
            Object timestamp = generic.get("timestamp");
            
            // Extract the data payload
            Object dataPayload = generic.get("data");
            
            final SpecificRecord specificPayload;
            if (dataPayload instanceof GenericRecord) {
                GenericRecord payloadRecord = (GenericRecord) dataPayload;
                String payloadClassName = payloadRecord.getSchema().getFullName();
                Class<?> payloadClass = Class.forName(payloadClassName);

                specificPayload = (SpecificRecord) payloadClass
                        .getDeclaredConstructor()
                        .newInstance();

                payloadRecord.getSchema().getFields().forEach(field -> {
                    Object value = payloadRecord.get(field.name());
                    try {
                        payloadClass.getMethod(
                            "set" + capitalize(field.name()),
                            value.getClass()
                        ).invoke(specificPayload, value);
                    } catch (Exception e) {
                        log.error("Failed to set field: {} on payload class: {} from topic {}: {}", 
                                field.name(), payloadClassName, topic, e.getMessage(), e);
                        throw new RuntimeException("Failed to set field: " + field.name(), e);
                    }
                });
            } else {
                specificPayload = null;
            }

            // Create EventWrapper with correct constructor
            EventWrapper<SpecificRecord> eventWrapper = new EventWrapper<>();
            eventWrapper.setEventId(eventId);
            eventWrapper.setEventType(eventType);
            eventWrapper.setVersion(version);
            eventWrapper.setTopic(topicName);
            eventWrapper.setData(specificPayload);
            eventWrapper.setTimestamp(timestamp != null ? 
                java.time.OffsetDateTime.parse(timestamp.toString()) : 
                java.time.OffsetDateTime.now());

            return eventWrapper;
        } catch (Exception e) {
            log.error("Avro deserialization failed for topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Avro deserialization failed for topic " + topic, e);
        }
    }

    @Override
    public void close() {
        innerDeserializer.close();
    }

    private String capitalize(String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
