package com.example.demo.kafka.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.kafka.metrics.KafkaMetricsService;
import com.example.demo.kafka.producer.KafkaEventProducer;

import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class KafkaMonitoringController {

    private final KafkaMetricsService kafkaMetricsService;
    private final KafkaEventProducer kafkaEventProducer;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getKafkaHealth() {
        log.info("Kafka health check requested");
        Map<String, Object> health = new HashMap<>();
        
        try {
            health.put("status", "UP");
            health.put("timestamp", System.currentTimeMillis());
            health.put("producer_metrics", kafkaMetricsService.getProducerMetrics());
            health.put("consumer_metrics", kafkaMetricsService.getConsumerMetrics());
            health.put("error_metrics", kafkaMetricsService.getErrorMetrics());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error getting Kafka health", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        log.info("Kafka metrics requested");
        return ResponseEntity.ok(kafkaMetricsService.getSystemHealth());
    }

    @GetMapping("/metrics/producer")
    public ResponseEntity<Map<String, Object>> getProducerMetrics() {
        log.info("Producer metrics requested");
        return ResponseEntity.ok(kafkaMetricsService.getProducerMetrics());
    }

    @GetMapping("/metrics/consumer")
    public ResponseEntity<Map<String, Object>> getConsumerMetrics() {
        log.info("Consumer metrics requested");
        return ResponseEntity.ok(kafkaMetricsService.getConsumerMetrics());
    }

    @GetMapping("/metrics/errors")
    public ResponseEntity<Map<String, Object>> getErrorMetrics() {
        log.info("Error metrics requested");
        return ResponseEntity.ok(kafkaMetricsService.getErrorMetrics());
    }

    @GetMapping("/metrics/topics")
    public ResponseEntity<Map<String, Object>> getAllTopicMetrics() {
        log.info("All topic metrics requested");
        return ResponseEntity.ok(kafkaMetricsService.getAllTopicMetrics());
    }

    @GetMapping("/metrics/topics/{topic}")
    public ResponseEntity<Map<String, Object>> getTopicMetrics(@PathVariable String topic) {
        log.info("Topic metrics requested for: {}", topic);
        Map<String, Object> metrics = kafkaMetricsService.getTopicMetrics(topic);
        
        if (metrics.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/producer/status")
    public ResponseEntity<Map<String, Object>> getProducerStatus() {
        log.info("Producer status requested");
        Map<String, Object> status = new HashMap<>();
        
        status.put("total_messages_sent", kafkaEventProducer.getTotalMessagesSent());
        status.put("successful_messages", kafkaEventProducer.getSuccessfulMessages());
        status.put("failed_messages", kafkaEventProducer.getFailedMessages());
        status.put("success_rate_percent", kafkaEventProducer.getSuccessRate());
        status.put("circuit_breaker_open", kafkaEventProducer.isCircuitBreakerOpenStatus());
        status.put("circuit_breaker_failures", kafkaEventProducer.getCircuitBreakerFailures());
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/producer/reset-circuit-breaker")
    public ResponseEntity<Map<String, Object>> resetCircuitBreaker() {
        log.info("Circuit breaker reset requested");
        Map<String, Object> response = new HashMap<>();
        
        // Note: This would require adding a reset method to KafkaEventProducer
        // For now, we'll just return the current status
        response.put("message", "Circuit breaker reset request received");
        response.put("current_status", kafkaEventProducer.isCircuitBreakerOpenStatus());
        response.put("current_failures", kafkaEventProducer.getCircuitBreakerFailures());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topics/active")
    public ResponseEntity<Map<String, Object>> getActiveTopics() {
        log.info("Active topics requested");
        Map<String, Object> response = new HashMap<>();
        
        // Get all topic metrics and filter active ones
        Map<String, Object> allTopicMetrics = kafkaMetricsService.getAllTopicMetrics();
        Map<String, Object> activeTopics = new HashMap<>();
        
        allTopicMetrics.forEach((topic, metrics) -> {
            if (metrics instanceof Map) {
                Map<String, Object> topicMetrics = (Map<String, Object>) metrics;
                Long lastMessageTime = (Long) topicMetrics.get("last_message_time");
                
                // Consider topic active if it had a message in the last 5 minutes
                if (lastMessageTime != null && 
                    System.currentTimeMillis() - lastMessageTime < 300000) {
                    activeTopics.put(topic, topicMetrics);
                }
            }
        });
        
        response.put("active_topics", activeTopics);
        response.put("total_active_topics", activeTopics.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/performance/summary")
    public ResponseEntity<Map<String, Object>> getPerformanceSummary() {
        log.info("Performance summary requested");
        Map<String, Object> summary = new HashMap<>();
        
        Map<String, Object> producerMetrics = kafkaMetricsService.getProducerMetrics();
        Map<String, Object> consumerMetrics = kafkaMetricsService.getConsumerMetrics();
        Map<String, Object> errorMetrics = kafkaMetricsService.getErrorMetrics();
        
        summary.put("producer_performance", Map.of(
            "messages_per_second", calculateMessagesPerSecond((Long) producerMetrics.get("total_messages")),
            "average_latency_ms", producerMetrics.get("average_latency_ms"),
            "success_rate_percent", producerMetrics.get("success_rate_percent")
        ));
        
        summary.put("consumer_performance", Map.of(
            "messages_per_second", calculateMessagesPerSecond((Long) consumerMetrics.get("total_messages")),
            "average_latency_ms", consumerMetrics.get("average_latency_ms"),
            "consumer_lag", consumerMetrics.get("consumer_lag")
        ));
        
        summary.put("system_health", Map.of(
            "error_rate_percent", errorMetrics.get("error_rate_percent"),
            "total_errors", errorMetrics.get("total_errors"),
            "uptime_seconds", kafkaMetricsService.getSystemHealth().get("uptime_seconds")
        ));
        
        return ResponseEntity.ok(summary);
    }

    private double calculateMessagesPerSecond(Long totalMessages) {
        // This is a simplified calculation - in a real system, you'd track this over time
        long uptimeSeconds = System.currentTimeMillis() / 1000;
        return uptimeSeconds > 0 ? (double) totalMessages / uptimeSeconds : 0.0;
    }
} 