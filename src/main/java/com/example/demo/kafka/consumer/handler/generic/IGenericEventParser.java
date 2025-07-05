package com.example.demo.kafka.consumer.handler.generic;

import org.apache.avro.specific.SpecificRecord;

public interface IGenericEventParser<Entity, TPayload extends SpecificRecord> {
    
    Entity parseAvro(TPayload payload, Class<Entity> entityClazz);

    Entity parseJson(Object jsonObject);
    
    Entity parseString(String value);

}
