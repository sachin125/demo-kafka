package com.example.demo.kafka.unused;
// package com.example.demo.kafka.producer.publisher.generic;

// import lombok.RequiredArgsConstructor;

// import com.example.demo.kafka.util.EventWrapperSerializer;
// import com.example.demo.kafka.factory.EventFactory;
// import com.example.demo.kafka.factory.EventWrapper;
// import com.example.demo.kafka.producer.util.KafkaEventProducer;

// import org.apache.avro.generic.GenericRecord;
// import org.apache.avro.specific.SpecificRecord;
// import org.springframework.stereotype.Component;

// @RequiredArgsConstructor
// @Component
// public class KafkaEventPublisher {

//     private final KafkaEventProducer kafkaEventProducer;

//     private final EventWrapperSerializer eventWrapperSerializer;

//     public void publishSpecificRecord(Object jpaEntity, String key, Class<?> clazz, String eventType) {
//         SpecificRecord avroRecord = eventWrapperSerializer.convertToAvroAndGetSpecificRecord(jpaEntity, clazz.getSimpleName());
//         String eventName = clazz.getSimpleName()+ eventType +"-event";
//         EventWrapper<SpecificRecord> event = EventFactory.create(eventName, avroRecord);
//         kafkaEventProducer.sendAvro(eventType, key, event);
//     }


//     public void publishGenericRecord(Object entity,String key, Class<?> clazz, String entityType) {
//         GenericRecord avroRecord = eventWrapperSerializer.convertToAvroAndGetGenericRecord(entity, entityType);
//         String eventName = clazz.getSimpleName()+ entityType +"-event";
//         EventWrapper<GenericRecord> event = EventFactory.create(eventName, avroRecord);
//         kafkaEventProducer.sendAvro(entityType, key, event);
//     }


// }