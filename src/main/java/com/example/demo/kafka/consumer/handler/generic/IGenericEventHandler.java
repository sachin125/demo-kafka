package com.example.demo.kafka.consumer.handler.generic;

import org.apache.avro.specific.SpecificRecord;

import com.example.demo.kafka.factory.EventWrapper;

public interface IGenericEventHandler<Entity, TPayload> {
    void handleCreate(Entity entity);
    void handleUpdate(Entity entity);
    void handleDelete(Entity entity);

    Entity handleAvro(EventWrapper<SpecificRecord> event);

    Entity handleJson(EventWrapper<Object> event);

    Entity handleString(EventWrapper<String> value);


}