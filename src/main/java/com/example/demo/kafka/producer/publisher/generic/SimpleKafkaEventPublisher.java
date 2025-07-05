package com.example.demo.kafka.producer.publisher.generic;

import org.apache.avro.specific.SpecificRecord;

import com.example.avro.AvroEventWrapper;
import com.example.demo.kafka.config.KafkaEventFormat;
import com.example.demo.kafka.factory.EventFactory;
import com.example.demo.kafka.factory.EventWrapper;
import com.example.demo.kafka.producer.KafkaEventProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class SimpleKafkaEventPublisher<Entity, TPayload> 
                implements IGenericEventPublisher<Entity> {

    protected final KafkaEventProducer kafkaEventProducer;

    private static final String DEFAULT_VERSION = "v1";

    protected abstract String extractKey(Entity entity);

    protected abstract TPayload toAvro(Entity entity);

    protected abstract String getEventSource();

    protected abstract Class<Entity> getEntityClass();

    protected String getEntityClassType() {
        return getEntityClass().getSimpleName().toLowerCase();
    }

    protected String getTopic(String eventType, KafkaEventFormat format) {
        String suffix;
        switch (format) {
            case AVRO:
                suffix = "-avro";
                break;
            case JSON:
                suffix = "-json";
                break;
            case STRING:
                suffix = "-string";
                break;
            default:
                suffix = "-event"; // default to event for backward compatibility
        }
        return getEntityClass().getSimpleName().toLowerCase() + "-" + eventType + suffix;
    }

    protected String getTopic(String eventType) {
        return getTopic(eventType, KafkaEventFormat.AVRO); // default to AVRO for backward compatibility
    }
    
    protected String toJson(Entity entity) {
        return com.example.common.JsonUtil.toJson(entity);
    }

    // Default: Avro
    public void publish(Entity entity, String eventSuffix) {
        publish(entity, eventSuffix, KafkaEventFormat.AVRO);
    }

    public void publish(Entity entity, String eventType, KafkaEventFormat format) {
        String key = extractKey(entity);
        String topic = getTopic(eventType, format);
        log.info("Entry @class SimpleKafkaEventPublisher @method publish topic: {} with event type: {} and format: {}", topic, eventType, format);
        switch (format) {
            case AVRO:
                TPayload avroPayload = toAvro(entity);
                com.example.avro.AvroEventWrapper avroEvent = com.example.demo.kafka.factory.EventFactory.createAvro(avroPayload, eventType, getEntityClassType(), getEventSource(), topic, DEFAULT_VERSION);
                kafkaEventProducer.sendAvro(topic, key, avroEvent);
                break;
            case JSON:
                String jsonPayload = toJson(entity);
                com.example.demo.kafka.factory.EventWrapper<String> jsonEvent = com.example.demo.kafka.factory.EventFactory.createJson(jsonPayload, eventType, getEntityClassType(), getEventSource(), topic, DEFAULT_VERSION);
                kafkaEventProducer.sendJson(topic, key, jsonEvent);
                break;
            case STRING:
                String stringPayload = entity.toString();
                kafkaEventProducer.sendString(topic, key, stringPayload);
                break;
        }
    }

    @Override
    public void publishCreate(Entity entity) {
        publish(entity, "create");
    }

    public void publishCreate(Entity entity, KafkaEventFormat format) {
        publish(entity, "create", format);
    }

    @Override
    public void publishUpdate(Entity entity) {
        publish(entity, "update");
    }

    public void publishUpdate(Entity entity, KafkaEventFormat format) {
        publish(entity, "update", format);
    }

    @Override
    public void publishDelete(Entity entity) {
        publish(entity, "delete");
    }

    public void publishDelete(Entity entity, KafkaEventFormat format) {
        publish(entity, "delete", format);
    }
}
