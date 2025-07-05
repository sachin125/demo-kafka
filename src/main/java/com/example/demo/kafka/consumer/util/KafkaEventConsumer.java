package com.example.demo.kafka.consumer.util;

import java.util.Optional;
import java.util.function.Function;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.*;

import org.springframework.stereotype.Service;

import com.example.demo.kafka.config.KafkaEventFormat;
import com.example.demo.kafka.config.KafkaTopicsProvider;
import com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler;
import com.example.demo.kafka.entity.EventProcessingRecord;
import com.example.demo.kafka.factory.EventWrapper;
import com.example.demo.kafka.repo.EventProcessingRecordRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class KafkaEventConsumer {

    private final EventProcessingRecordRepository recordRepository;
    private final KafkaEventHandlerRegistry handlerRegistry;
    private final KafkaTopicsProvider kafkaTopicsProvider;

    @KafkaListener(
        topics = "#{@kafkaTopicsProvider.getAvroTopics()}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "avroKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeAvro(ConsumerRecord<String, com.example.avro.AvroEventWrapper> record, Acknowledgment acknowledgment) {
        processMessageAvro(record, acknowledgment, KafkaEventFormat.AVRO, this::handleAvroEvent);
    }

    @KafkaListener(
        topics = "#{@kafkaTopicsProvider.getJsonTopics()}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "jsonKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeJson(ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>> record, Acknowledgment acknowledgment) {
        processMessageJsonString(record, acknowledgment, KafkaEventFormat.JSON, this::handleJsonEvent);
    }

    @KafkaListener(
        topics = "#{@kafkaTopicsProvider.getStringTopics()}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeString(ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>> record, Acknowledgment acknowledgment) {
        processMessageJsonString(record, acknowledgment, KafkaEventFormat.STRING, this::handleStringEvent);
    }

    // AVRO processing
    private <T> void processMessageAvro(ConsumerRecord<String, com.example.avro.AvroEventWrapper> record, 
                                  Acknowledgment acknowledgment, 
                                  KafkaEventFormat format, 
                                  java.util.function.Function<ConsumerRecord<String, com.example.avro.AvroEventWrapper>, Void> eventHandler) {
        String messageId = record.key() != null ? record.key() : generateMessageId(record);
        String topic = record.topic();
        String entityType = extractEntityTypeFromTopic(topic);
        com.example.avro.AvroEventWrapper event = record.value();
        log.info("Entry @class KafkaEventConsumer @method processMessageAvro messageId: {} topic: {} entityType: {} event: {}", messageId, topic, entityType, event);
        try {
            // Check for duplicate messages
            if (recordRepository.existsById(messageId)) {
                log.warn("Duplicate message detected: ID={}, Topic={}. Skipping processing.", messageId, record.topic());
                acknowledgment.acknowledge();
                return;
            }
            log.info("Processing {} event: MessageID={}, Operation={}, EntityType={}, Topic={}", 
                    format, messageId, event.getEventType(), entityType, record.topic());
            eventHandler.apply(record);
            saveProcessingRecordAvro(messageId, record, event, entityType);
        } catch (Exception e) {
            log.error("Unrecoverable error processing {} event: MessageID={}. Sending to DLT: {}", 
                    format, messageId, record.topic() + "-dlt", e);
        }
        acknowledgment.acknowledge();
    }

    // JSON/STRING processing
    private <T> void processMessageJsonString(ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>> record, 
                                  Acknowledgment acknowledgment, 
                                  KafkaEventFormat format, 
                                  java.util.function.Function<ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>>, Void> eventHandler) {
        String messageId = record.key() != null ? record.key() : generateMessageId(record);
        String topic = record.topic();
        String entityType = extractEntityTypeFromTopic(topic);
        com.example.demo.kafka.factory.EventWrapper<String> event = record.value();
        log.info("Entry @class KafkaEventConsumer @method processMessageJsonString messageId: {} topic: {} entityType: {} event: {}", messageId, topic, entityType, event);
        try {
            if (recordRepository.existsById(messageId)) {
                log.warn("Duplicate message detected: ID={}, Topic={}. Skipping processing.", messageId, record.topic());
                acknowledgment.acknowledge();
                return;
            }
            log.info("Processing {} event: MessageID={}, Operation={}, EntityType={}, Topic={}", 
                    format, messageId, event.getEventType(), entityType, record.topic());
            eventHandler.apply(record);
            saveProcessingRecord(messageId, record, event, entityType);
        } catch (Exception e) {
            log.error("Unrecoverable error processing {} event: MessageID={}. Sending to DLT: {}", 
                    format, messageId, record.topic() + "-dlt", e);
        }
        acknowledgment.acknowledge();
    }

    // Handler methods
    private Void handleAvroEvent(ConsumerRecord<String, com.example.avro.AvroEventWrapper> record) {
        String entityType = extractEntityTypeFromTopic(record.topic());
        java.util.Optional<com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler<?, ?>> optionalHandler = handlerRegistry.getHandler(entityType);
        if (optionalHandler.isPresent()) {
            // You may need to adapt this to your handler's expected method
            // For example, handler.handleAvroAvroWrapper(record.value());
            // Or convert AvroEventWrapper to your entity and call handleCreate/update/delete
        }
        return null;
    }

    private Void handleJsonEvent(ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>> record) {
        String entityType = extractEntityTypeFromTopic(record.topic());
        java.util.Optional<com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler<?, ?>> optionalHandler = handlerRegistry.getHandler(entityType);
        if (optionalHandler.isPresent()) {
            optionalHandler.get().handleJson((com.example.demo.kafka.factory.EventWrapper<Object>) (Object) record.value());
        }
        return null;
    }

    private Void handleStringEvent(ConsumerRecord<String, com.example.demo.kafka.factory.EventWrapper<String>> record) {
        String entityType = extractEntityTypeFromTopic(record.topic());
        java.util.Optional<com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler<?, ?>> optionalHandler = handlerRegistry.getHandler(entityType);
        if (optionalHandler.isPresent()) {
            optionalHandler.get().handleString(record.value());
        }
        return null;
    }

    /**
     * Utility methods
     */
    private String generateMessageId(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    private <T> void saveProcessingRecord(String messageId, ConsumerRecord<String, EventWrapper<T>> record,
                                        EventWrapper<T> event, String entityType) {
        log.info("Entry @class KafkaEventConsumer @method saveProcessingRecord messageId: {} topic: {} entityType: {} event: {}", messageId, record.topic(), entityType, event);
        EventProcessingRecord processingRecord = new EventProcessingRecord();
        processingRecord.setMessageId(messageId);
        processingRecord.setTopic(record.topic());
        processingRecord.setOffset(record.offset());
        processingRecord.setPartitionNumber(record.partition());
        processingRecord.setOperation(event.getEventType());
        processingRecord.setEntityType(entityType);
        processingRecord.setProcessedTimestamp(System.currentTimeMillis());
        recordRepository.save(processingRecord);
    }

    private void saveProcessingRecordAvro(String messageId, ConsumerRecord<String, com.example.avro.AvroEventWrapper> record, com.example.avro.AvroEventWrapper event, String entityType) {
        log.info("Entry @class KafkaEventConsumer @method saveProcessingRecordAvro messageId: {} topic: {} entityType: {} event: {}", messageId, record.topic(), entityType, event);
        com.example.demo.kafka.entity.EventProcessingRecord processingRecord = new com.example.demo.kafka.entity.EventProcessingRecord();
        processingRecord.setMessageId(messageId);
        processingRecord.setTopic(record.topic());
        processingRecord.setOffset(record.offset());
        processingRecord.setPartitionNumber(record.partition());
        processingRecord.setOperation(event.getEventType() != null ? event.getEventType().toString() : null);
        processingRecord.setEntityType(entityType);
        processingRecord.setProcessedTimestamp(System.currentTimeMillis());
        recordRepository.save(processingRecord);
    }

    private String extractEntityTypeFromTopic(String topic) {
        if (topic == null) return "";
        int dashIdx = topic.indexOf('-');
        return dashIdx > 0 ? topic.substring(0, dashIdx) : topic;
    }
}