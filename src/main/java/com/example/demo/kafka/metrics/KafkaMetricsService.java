package com.example.demo.kafka.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.example.demo.kafka.producer.KafkaEventProducer;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaMetricsService {

    private final KafkaEventProducer kafkaEventProducer;

    // Producer metrics
    private final AtomicLong totalProducerMessages = new AtomicLong(0);
    private final AtomicLong totalProducerBytes = new AtomicLong(0);
    private final AtomicLong producerLatencySum = new AtomicLong(0);
    private final AtomicLong producerLatencyCount = new AtomicLong(0);
    private final AtomicInteger activeProducerConnections = new AtomicInteger(0);

    // Consumer metrics
    private final AtomicLong totalConsumerMessages = new AtomicLong(0);
    private final AtomicLong totalConsumerBytes = new AtomicLong(0);
    private final AtomicLong consumerLatencySum = new AtomicLong(0);
    private final AtomicLong consumerLatencyCount = new AtomicLong(0);
    private final AtomicInteger activeConsumerConnections = new AtomicInteger(0);
    private final AtomicLong consumerLag = new AtomicLong(0);

    // Error metrics
    private final AtomicLong totalErrors = new AtomicLong(0);
    private final AtomicLong deserializationErrors = new AtomicLong(0);
    private final AtomicLong networkErrors = new AtomicLong(0);
    private final AtomicLong timeoutErrors = new AtomicLong(0);

    // Topic-specific metrics
    private final Map<String, TopicMetrics> topicMetrics = new ConcurrentHashMap<>();

    // Performance tracking
    private final Map<String, Long> lastMessageTime = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> topicMessageCounts = new ConcurrentHashMap<>();

    public void recordProducerMessage(String topic, String key, long messageSize, long latencyMs) {
        totalProducerMessages.incrementAndGet();
        totalProducerBytes.addAndGet(messageSize);
        producerLatencySum.addAndGet(latencyMs);
        producerLatencyCount.incrementAndGet();

        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        metrics.recordProducerMessage(messageSize, latencyMs);

        lastMessageTime.put(topic, System.currentTimeMillis());
        topicMessageCounts.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();

        log.debug("Recorded producer message: topic={}, key={}, size={}, latency={}ms", 
                 topic, key, messageSize, latencyMs);
    }

    public void recordConsumerMessage(String topic, String key, long messageSize, long latencyMs) {
        totalConsumerMessages.incrementAndGet();
        totalConsumerBytes.addAndGet(messageSize);
        consumerLatencySum.addAndGet(latencyMs);
        consumerLatencyCount.incrementAndGet();

        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        metrics.recordConsumerMessage(messageSize, latencyMs);

        lastMessageTime.put(topic, System.currentTimeMillis());
        topicMessageCounts.computeIfAbsent(topic, k -> new AtomicLong(0)).incrementAndGet();

        log.debug("Recorded consumer message: topic={}, key={}, size={}, latency={}ms", 
                 topic, key, messageSize, latencyMs);
    }

    public void recordError(String topic, String errorType, String errorMessage) {
        totalErrors.incrementAndGet();

        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        metrics.recordError(errorType);

        switch (errorType.toLowerCase()) {
            case "deserialization":
                deserializationErrors.incrementAndGet();
                break;
            case "network":
                networkErrors.incrementAndGet();
                break;
            case "timeout":
                timeoutErrors.incrementAndGet();
                break;
        }

        log.warn("Recorded error: topic={}, type={}, message={}", topic, errorType, errorMessage);
    }

    public void setConsumerLag(String topic, long lag) {
        consumerLag.set(lag);
        TopicMetrics metrics = topicMetrics.computeIfAbsent(topic, k -> new TopicMetrics());
        metrics.setConsumerLag(lag);
    }

    public void setActiveProducerConnections(int count) {
        activeProducerConnections.set(count);
    }

    public void setActiveConsumerConnections(int count) {
        activeConsumerConnections.set(count);
    }

    public Map<String, Object> getProducerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_messages", totalProducerMessages.get());
        metrics.put("total_bytes", totalProducerBytes.get());
        metrics.put("average_latency_ms", getAverageLatency(producerLatencySum.get(), producerLatencyCount.get()));
        metrics.put("active_connections", activeProducerConnections.get());
        metrics.put("success_rate_percent", kafkaEventProducer.getSuccessRate());
        metrics.put("circuit_breaker_open", kafkaEventProducer.isCircuitBreakerOpenStatus());
        metrics.put("circuit_breaker_failures", kafkaEventProducer.getCircuitBreakerFailures());
        return metrics;
    }

    public Map<String, Object> getConsumerMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_messages", totalConsumerMessages.get());
        metrics.put("total_bytes", totalConsumerBytes.get());
        metrics.put("average_latency_ms", getAverageLatency(consumerLatencySum.get(), consumerLatencyCount.get()));
        metrics.put("active_connections", activeConsumerConnections.get());
        metrics.put("consumer_lag", consumerLag.get());
        return metrics;
    }

    public Map<String, Object> getErrorMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_errors", totalErrors.get());
        metrics.put("deserialization_errors", deserializationErrors.get());
        metrics.put("network_errors", networkErrors.get());
        metrics.put("timeout_errors", timeoutErrors.get());
        metrics.put("error_rate_percent", calculateErrorRate());
        return metrics;
    }

    public Map<String, Object> getTopicMetrics(String topic) {
        TopicMetrics metrics = topicMetrics.get(topic);
        if (metrics == null) {
            return new HashMap<>();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("producer_messages", metrics.getProducerMessages());
        result.put("consumer_messages", metrics.getConsumerMessages());
        result.put("producer_bytes", metrics.getProducerBytes());
        result.put("consumer_bytes", metrics.getConsumerBytes());
        result.put("producer_latency_ms", metrics.getProducerLatency());
        result.put("consumer_latency_ms", metrics.getConsumerLatency());
        result.put("errors", metrics.getErrors());
        result.put("consumer_lag", metrics.getConsumerLag());
        result.put("last_message_time", lastMessageTime.getOrDefault(topic, 0L));
        result.put("message_count", topicMessageCounts.getOrDefault(topic, new AtomicLong(0)).get());
        return result;
    }

    public Map<String, Object> getAllTopicMetrics() {
        Map<String, Object> allMetrics = new HashMap<>();
        topicMetrics.forEach((topic, metrics) -> {
            allMetrics.put(topic, getTopicMetrics(topic));
        });
        return allMetrics;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("producer_metrics", getProducerMetrics());
        health.put("consumer_metrics", getConsumerMetrics());
        health.put("error_metrics", getErrorMetrics());
        health.put("topic_metrics", getAllTopicMetrics());
        health.put("uptime_seconds", getUptimeSeconds());
        return health;
    }

    private double getAverageLatency(long sum, long count) {
        return count > 0 ? (double) sum / count : 0.0;
    }

    private double calculateErrorRate() {
        long totalMessages = totalProducerMessages.get() + totalConsumerMessages.get();
        return totalMessages > 0 ? (double) totalErrors.get() / totalMessages * 100 : 0.0;
    }

    private long getUptimeSeconds() {
        // This would typically come from application startup time
        return System.currentTimeMillis() / 1000;
    }

    // Inner class for topic-specific metrics
    private static class TopicMetrics {
        private final AtomicLong producerMessages = new AtomicLong(0);
        private final AtomicLong consumerMessages = new AtomicLong(0);
        private final AtomicLong producerBytes = new AtomicLong(0);
        private final AtomicLong consumerBytes = new AtomicLong(0);
        private final AtomicLong producerLatencySum = new AtomicLong(0);
        private final AtomicLong producerLatencyCount = new AtomicLong(0);
        private final AtomicLong consumerLatencySum = new AtomicLong(0);
        private final AtomicLong consumerLatencyCount = new AtomicLong(0);
        private final AtomicLong errors = new AtomicLong(0);
        private final AtomicLong consumerLag = new AtomicLong(0);

        public void recordProducerMessage(long messageSize, long latencyMs) {
            producerMessages.incrementAndGet();
            producerBytes.addAndGet(messageSize);
            producerLatencySum.addAndGet(latencyMs);
            producerLatencyCount.incrementAndGet();
        }

        public void recordConsumerMessage(long messageSize, long latencyMs) {
            consumerMessages.incrementAndGet();
            consumerBytes.addAndGet(messageSize);
            consumerLatencySum.addAndGet(latencyMs);
            consumerLatencyCount.incrementAndGet();
        }

        public void recordError(String errorType) {
            errors.incrementAndGet();
        }

        public void setConsumerLag(long lag) {
            consumerLag.set(lag);
        }

        // Getters
        public long getProducerMessages() { return producerMessages.get(); }
        public long getConsumerMessages() { return consumerMessages.get(); }
        public long getProducerBytes() { return producerBytes.get(); }
        public long getConsumerBytes() { return consumerBytes.get(); }
        public double getProducerLatency() { 
            return producerLatencyCount.get() > 0 ? (double) producerLatencySum.get() / producerLatencyCount.get() : 0.0; 
        }
        public double getConsumerLatency() { 
            return consumerLatencyCount.get() > 0 ? (double) consumerLatencySum.get() / consumerLatencyCount.get() : 0.0; 
        }
        public long getErrors() { return errors.get(); }
        public long getConsumerLag() { return consumerLag.get(); }
    }
} 