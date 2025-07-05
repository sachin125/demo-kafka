package com.example.demo.kafka.consumer.handler.generic;

import java.util.Optional;

import org.apache.avro.specific.SpecificRecord;

import com.example.demo.kafka.config.KafkaEventFormat;
import com.example.demo.kafka.factory.EventWrapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SimpleKafkaEventHandler<Entity, TPayload extends SpecificRecord> 
    implements IGenericEventHandler<Entity,TPayload> { //, IGenericEventParser<Entity, TPayload>

    protected abstract Entity toEntity(TPayload payload);
    public abstract Class<Entity> getEntityClass();
    protected abstract Class<TPayload> getPayloadClass();

    
    protected void auditEvent(EventWrapper<?> event, String topic) {
       // String entityType = event.getData() instanceof SpecificRecord ?((SpecificRecord) event.getData()).getSchema().getName(): event.getData().getClass().getSimpleName();
        String entityType = getEntityClass().getSimpleName();
        log.info("Auditing event: Operation={}, EntityType={}, Topic={}, Data={}",
                    event.getEventType(), entityType, topic, event.getData());
            
    }

    public void handleEvent(EventWrapper<?> event, KafkaEventFormat format, String topic) {
        if (event == null || event.getData() == null) {
            log.warn("Event or payload is null. Skipping.");
            return;
        }
        auditEvent(event, topic);
        Entity entity = null;
        switch (format) {
            case KafkaEventFormat.AVRO:
                entity = handleAvro((EventWrapper<SpecificRecord>)event);
                break;
            case KafkaEventFormat.JSON:
                entity = handleJson((EventWrapper<Object>)event);
                break;
            case KafkaEventFormat.STRING:
                entity = handleString((EventWrapper<String>)event);
                break;


        }
        this.eventOperation(entity, event.getEventType());
    }

    private void eventOperation(Entity entity, String eventType){
        String op = Optional.ofNullable(eventType)
                    .map(String::toLowerCase)
                    .orElse("unknown");

        switch (op) {
            case "create":
                handleCreate(entity);
                break;
            case "update":
                handleUpdate(entity);
                break;
            case "delete":
                handleDelete(entity);
                break;
            default:
                log.warn("Unknown operation: {}", op);
        }
    }

    @Override
    public Entity handleAvro(EventWrapper<SpecificRecord> event) {
        SpecificRecord specificRecord = event.getData();
        if (specificRecord == null) {
            log.warn("[AVRO] Received null object for {}", getEntityClass().getSimpleName());
            return null;
        }
        TPayload payload;
        try {
            payload = (TPayload) specificRecord;
        } catch (ClassCastException e) {
            log.error("[AVRO] Payload type mismatch: expected={}, actual={}",
                    getPayloadClass().getSimpleName(), specificRecord.getClass().getSimpleName(), e);
            throw new IllegalArgumentException("Invalid Avro payload type for " + getEntityClass().getSimpleName(), e);
        }
        
        Entity entity = toEntity(payload);
        log.info("[AVRO] Handling {}: {}", getEntityClass().getSimpleName(), entity);
        this.eventOperation(entity, event.getEventType());
        return entity;
    }

    @Override
    public Entity handleJson(EventWrapper<Object> event) {
        Object objectJson = event.getData();
        if (objectJson == null) {
            log.warn("[JSON] Received null object for {}", getEntityClass().getSimpleName());
            return null;
        }
        Entity entity = null;
        try {
            if (getEntityClass().isInstance(objectJson)) {
                entity = getEntityClass().cast(objectJson);
            } else if (objectJson instanceof String) {
                entity = com.example.common.JsonUtil.fromJson((String) objectJson, getEntityClass());
            } else {
                entity = com.example.common.JsonUtil.fromJson(com.example.common.JsonUtil.toJson(objectJson), getEntityClass());
            }
            log.info("[JSON] Handling {}: {}", getEntityClass().getSimpleName(), entity);
        } catch (Exception e) {
            log.warn("[JSON] Could not convert to {}: {}", getEntityClass().getSimpleName(), e.getMessage());
        }
        this.eventOperation(entity, event.getEventType());
        return entity;
        // Optionally call handleCreate/update/delete if you have event type info
    }

    @Override
    public Entity handleString(EventWrapper<String> event) {
        String value = event.getData();
        if (value == null || value.isBlank()) {
            log.warn("[STRING] Received blank string for {}", getEntityClass().getSimpleName());
            return null;
        }
        Entity entity = null;
        try {
            entity = com.example.common.JsonUtil.fromJson(value, getEntityClass());
            log.info("[STRING] Parsed {} from JSON string: {}", getEntityClass().getSimpleName(), entity);
        } catch (Exception e) {
            log.warn("[STRING] Could not parse as JSON, using raw string. Error: {}", e.getMessage());
            log.info("[STRING] Handling raw string for {}: {}", getEntityClass().getSimpleName(), value);
        }
        this.eventOperation(entity, event.getEventType());
        return entity;
    }
}
