package com.example.demo.kafka.factory;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.avro.AvroEventWrapper;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
@Component
public class EventFactory {
    

    public static <T> AvroEventWrapper createAvro(T payload, String eventType,
            String entityType, String source, String topic, String version){

        AvroEventWrapper eventWrapper = new AvroEventWrapper();
        eventWrapper.setEventId(UUID.randomUUID().toString());
        eventWrapper.setEventType(eventType);
        eventWrapper.setData(payload);
        eventWrapper.setEntityType(entityType);
        eventWrapper.setSource(source);
        eventWrapper.setTopic(topic);
        eventWrapper.setVersion(version);
        eventWrapper.setTraceId(UUID.randomUUID().toString());
        eventWrapper.setProducerRegion("us-east");
        eventWrapper.setRetryCount(0);
        eventWrapper.setTtl(3600);
        eventWrapper.setTimestamp(OffsetDateTime.now().toString());
        return eventWrapper;
    }

    public static <T> EventWrapper<T> createJson(T payload, String eventType,
            String entityType, String source, String topic, String version){

        EventWrapper<T> eventWrapper = new EventWrapper<>();
        eventWrapper.setEventId(UUID.randomUUID().toString());
        eventWrapper.setEventType(eventType);
        eventWrapper.setEntityType(entityType);
        eventWrapper.setData(payload);
        eventWrapper.setSource(source);
        eventWrapper.setTopic(topic);
        eventWrapper.setVersion(version);
        eventWrapper.setTraceId(UUID.randomUUID().toString());
        eventWrapper.setProducerRegion("us-east");
        eventWrapper.setRetryCount(0);
        eventWrapper.setTtl(3600);
        eventWrapper.setTimestamp(OffsetDateTime.now());
        return eventWrapper;
    }
    
}
