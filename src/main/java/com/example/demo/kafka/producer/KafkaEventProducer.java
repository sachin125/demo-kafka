package com.example.demo.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.example.avro.AvroEventWrapper;
import com.example.demo.kafka.factory.EventWrapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
@Component
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> jsonKafkaTemplate;
    private final KafkaTemplate<String, Object> avroKafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;

    // Metrics and monitoring
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong successfulMessages = new AtomicLong(0);
    private final AtomicLong failedMessages = new AtomicLong(0);
    private final AtomicInteger circuitBreakerFailures = new AtomicInteger(0);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 10;
    private static final long CIRCUIT_BREAKER_RESET_TIME = TimeUnit.MINUTES.toMillis(5);
    private volatile long lastFailureTime = 0;

    public <T> void sendAvro(String topic, String key, AvroEventWrapper event) {
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker is open, skipping message to topic: {}", topic);
            return;
        }

        log.info("Entry @class KafkaEventProducer @method sendAvro topic [{}] with key [{}]: {}", topic, key, event);
        totalMessagesSent.incrementAndGet();
        
        CompletableFuture<SendResult<String, Object>> future = avroKafkaTemplate.send(topic, key, event);
    
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(topic, key, ex, "Avro");
                log.error("Failed @class KafkaEventProducer @method sendAvro to topic [{}] with key [{}]: {}", 
                         topic, key, ex.getMessage());
            } else {
                handleSuccess();
                log.info("Successfully @class KafkaEventProducer @method sendAvro topic [{}] with offset [{}]", 
                         topic, result.getRecordMetadata().offset());
            }
        });
    }

    public <T> void sendJson(String topic, String key, EventWrapper<T> event) {
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker is open, skipping message to topic: {}", topic);
            return;
        }

        log.info("Producing JSON event to topic [{}] with key [{}]: {}", topic, key, event);
        totalMessagesSent.incrementAndGet();
        
        CompletableFuture<SendResult<String, Object>> future = jsonKafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(topic, key, ex, "JSON");
                log.error("Failed to send JSON message to topic [{}] with key [{}]: {}", topic, key, ex.getMessage());
            } else {
                handleSuccess();
                log.info("Successfully sent JSON message to topic [{}] with offset [{}]", topic, result.getRecordMetadata().offset());
            }
        });
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2.0))
    public void sendString(String topic, String key, String value) {
        if (isCircuitBreakerOpen()) {
            log.warn("Circuit breaker is open, skipping message to topic: {}", topic);
            return;
        }

        log.info("Producing String event to topic [{}] with key [{}]: {}", topic, key, value);
        totalMessagesSent.incrementAndGet();
        
        CompletableFuture<SendResult<String, String>> future = stringKafkaTemplate.send(topic, key, value);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                handleFailure(topic, key, ex, "String");
                log.error("Failed to send String message to topic [{}] with key [{}]: {}", topic, key, ex.getMessage());
            } else {
                handleSuccess();
                log.info("Successfully sent String message to topic [{}] with offset [{}]", topic, result.getRecordMetadata().offset());
            }
        });
    }

    // Circuit breaker implementation
    private boolean isCircuitBreakerOpen() {
        if (circuitBreakerFailures.get() >= CIRCUIT_BREAKER_THRESHOLD) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
            if (timeSinceLastFailure < CIRCUIT_BREAKER_RESET_TIME) {
                return true; // Circuit breaker is open
            } else {
                // Reset circuit breaker
                circuitBreakerFailures.set(0);
                log.info("Circuit breaker reset after {} ms", timeSinceLastFailure);
            }
        }
        return false;
    }

    private void handleFailure(String topic, String key, Throwable ex, String format) {
        failedMessages.incrementAndGet();
        circuitBreakerFailures.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        
        log.error("Failed to send {} message to topic [{}] with key [{}]: {}", 
                 format, topic, key, ex.getMessage());
    }

    private void handleSuccess() {
        successfulMessages.incrementAndGet();
        // Reset circuit breaker on success
        if (circuitBreakerFailures.get() > 0) {
            circuitBreakerFailures.decrementAndGet();
        }
    }

    // Metrics and monitoring methods
    public long getTotalMessagesSent() {
        return totalMessagesSent.get();
    }

    public long getSuccessfulMessages() {
        return successfulMessages.get();
    }

    public long getFailedMessages() {
        return failedMessages.get();
    }

    public double getSuccessRate() {
        long total = totalMessagesSent.get();
        return total > 0 ? (double) successfulMessages.get() / total * 100 : 0.0;
    }

    public boolean isCircuitBreakerOpenStatus() {
        return isCircuitBreakerOpen();
    }

    public int getCircuitBreakerFailures() {
        return circuitBreakerFailures.get();
    }
}
