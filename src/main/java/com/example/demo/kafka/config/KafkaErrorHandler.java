package com.example.demo.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class KafkaErrorHandler implements CommonErrorHandler {
    
    private KafkaTemplate<String, Object> avroKafkaTemplate;
    private KafkaTemplate<String, Object> jsonKafkaTemplate;
    private KafkaTemplate<String, String> stringKafkaTemplate;
    private ErrorHandlingConfig errorConfig;

    public KafkaErrorHandler(@Qualifier("avroKafkaTemplate") KafkaTemplate<String, Object> avroKafkaTemplate,
                           @Qualifier("jsonKafkaTemplate") KafkaTemplate<String, Object> jsonKafkaTemplate,
                           @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> stringKafkaTemplate,
                           ErrorHandlingConfig errorConfig) {
        this.avroKafkaTemplate = avroKafkaTemplate;
        this.jsonKafkaTemplate = jsonKafkaTemplate;
        this.stringKafkaTemplate = stringKafkaTemplate;
        this.errorConfig = errorConfig;
    }

        

    
    // Track retry attempts per message
    private final Map<String, Integer> retryAttempts = new ConcurrentHashMap<>();

    @Override
    public boolean handleOne(Exception thrownException, ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, MessageListenerContainer container) {
        String topic = record.topic();
        String key = record.key() != null ? record.key().toString() : "null";
        String messageId = generateMessageId(record);
        
        log.error("Error processing message: Topic={}, Key={}, MessageId={}, Error={}", 
                topic, key, messageId, thrownException.getMessage(), thrownException);

        try {
            // Check if we should retry
            if (shouldRetry(messageId, thrownException)) {
                handleRetry(record, consumer, messageId);
                return true; // Don't commit offset, retry will happen
            } else {
                // Max retries exceeded, send to DLT
                handleDeadLetter(record, thrownException, messageId);
                return false; // Commit offset, message processed (sent to DLT)
            }
        } catch (Exception e) {
            log.error("Error in error handler for message: Topic={}, Key={}, MessageId={}", 
                    topic, key, messageId, e);
            return false; // Commit offset to avoid infinite loop
        }
    }

    @Override
    public void handleOtherException(Exception thrownException, Consumer<?, ?> consumer, MessageListenerContainer container, boolean batchListener) {
        log.error("Error in Kafka consumer: {}", thrownException.getMessage(), thrownException);
        
        // For batch listeners or other exceptions, we might want to stop the container
        if (isFatalException(thrownException)) {
            log.error("Fatal error detected, stopping container: {}", container.getListenerId());
            container.stop();
        }
    }

    private boolean shouldRetry(String messageId, Exception exception) {
        int attempts = retryAttempts.getOrDefault(messageId, 0);
        
        // Don't retry for certain exceptions
        if (isNonRetryableException(exception)) {
            log.warn("Non-retryable exception for messageId={}: {}", messageId, exception.getClass().getSimpleName());
            return false;
        }
        
        return attempts < errorConfig.getRetry().getMaxAttempts();
    }

    private boolean isNonRetryableException(Exception exception) {
        return errorConfig.getRetry().getNonRetryableExceptions().contains(exception.getClass().getName());
    }

    private boolean isFatalException(Exception exception) {
        return exception instanceof SecurityException ||
               exception instanceof RuntimeException && 
               (exception.getCause() instanceof OutOfMemoryError || 
                exception.getCause() instanceof StackOverflowError);
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    private void handleRetry(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer, String messageId) {
        String topic = record.topic();
        String key = record.key() != null ? record.key().toString() : "null";
        
        // Increment retry count
        int attempts = retryAttempts.getOrDefault(messageId, 0) + 1;
        retryAttempts.put(messageId, attempts);
        
        log.warn("Retrying message: Topic={}, Key={}, MessageId={}, Attempt={}/{}", 
                topic, key, messageId, attempts, errorConfig.getRetry().getMaxAttempts());
        
        // Pause consumer for this partition to allow retry
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        consumer.pause(List.of(partition));
        
        // Schedule resume after delay
        long delayMs = (long) (errorConfig.getRetry().getInitialDelayMs() * Math.pow(errorConfig.getRetry().getMultiplier(), attempts - 1));
        delayMs = Math.min(delayMs, errorConfig.getRetry().getMaxDelayMs());
        scheduleResume(consumer, partition, delayMs);
    }

    private void scheduleResume(Consumer<?, ?> consumer, TopicPartition partition, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                consumer.resume(List.of(partition));
                log.debug("Resumed consumer for partition: {}", partition);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while scheduling resume for partition: {}", partition);
            }
        }).start();
    }

    private void handleDeadLetter(ConsumerRecord<?, ?> record, Exception exception, String messageId) {
        String topic = record.topic();
        String dltTopic = topic + errorConfig.getDlt().getSuffix();
        String key = record.key() != null ? record.key().toString() : "null";
        
        log.warn("Sending message to DLT: OriginalTopic={}, DltTopic={}, Key={}, MessageId={}, Attempts={}", 
                topic, dltTopic, key, messageId, retryAttempts.getOrDefault(messageId, 0));
        
        try {
            // Create comprehensive DLT message with enhanced error information
            com.example.demo.kafka.entity.DeadLetterMessage dltMessage = com.example.demo.kafka.entity.DeadLetterMessage.builder()
                    .originalTopic(topic)
                    .originalKey(key)
                    .originalValue(record.value())
                    .originalHeaders(record.headers())
                    .errorMessage(exception.getMessage())
                    .errorClass(exception.getClass().getSimpleName())
                    .errorStackTrace(getStackTrace(exception))
                    .retryAttempts(retryAttempts.getOrDefault(messageId, 0))
                    .timestamp(System.currentTimeMillis())
                    .consumerGroupId("app-name-group") // This should come from config
                    .consumerId("consumer-" + System.currentTimeMillis())
                    .originalOffset(record.offset())
                    .originalPartition(record.partition())
                    .originalTimestamp(record.timestamp())
                    .addContext("error_handling_timestamp", System.currentTimeMillis())
                    .addContext("error_handling_thread", Thread.currentThread().getName())
                    .build();
            
            // Send to DLT - use appropriate template based on original topic
            if (topic.contains("json")) {
                jsonKafkaTemplate.send(dltTopic, key, dltMessage);
            } else if (topic.contains("string")) {
                stringKafkaTemplate.send(dltTopic, key, dltMessage.toString());
            } else {
                // Default to Avro template
                avroKafkaTemplate.send(dltTopic, key, dltMessage);
            }
            
            // Clean up retry tracking
            retryAttempts.remove(messageId);
            
            log.info("Successfully sent message to DLT: Topic={}, Key={}, MessageId={}", dltTopic, key, messageId);
            
        } catch (Exception e) {
            log.error("Failed to send message to DLT: Topic={}, Key={}, MessageId={}", dltTopic, key, messageId, e);
            // Don't throw here to avoid infinite loop
        }
    }

    private String generateMessageId(ConsumerRecord<?, ?> record) {
        return record.topic() + "-" + record.partition() + "-" + record.offset();
    }

    // Clean up retry attempts periodically
    public void cleanupRetryAttempts() {
        int beforeSize = retryAttempts.size();
        retryAttempts.entrySet().removeIf(entry -> {
            // Remove entries older than 1 hour (you can adjust this)
            return System.currentTimeMillis() - entry.getValue() > 3600000;
        });
        int afterSize = retryAttempts.size();
        if (beforeSize != afterSize) {
            log.debug("Cleaned up {} retry attempt entries", beforeSize - afterSize);
        }
    }

    private String getStackTrace(Exception exception) {
        if (errorConfig.getDlt().isIncludeStackTrace()) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            return sw.toString();
        }
        return null;
    }

    // Get retry statistics
    public Map<String, Object> getRetryStatistics() {
        return Map.of(
                "activeRetryAttempts", retryAttempts.size(),
                "maxRetryAttempts", errorConfig.getRetry().getMaxAttempts(),
                "retryDelayMs", errorConfig.getRetry().getInitialDelayMs()
        );
    }
} 