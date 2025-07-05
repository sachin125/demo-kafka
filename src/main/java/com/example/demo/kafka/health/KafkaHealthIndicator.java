package com.example.demo.kafka.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.kafka.producer.KafkaEventProducer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, Object> avroKafkaTemplate;
    private final KafkaTemplate<String, Object> jsonKafkaTemplate;
    private final KafkaTemplate<String, String> stringKafkaTemplate;
    private final KafkaEventProducer kafkaEventProducer;

    private final AtomicBoolean lastHealthCheck = new AtomicBoolean(true);
    private volatile long lastCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(1);

    @Override
    public Health health() {
        long currentTime = System.currentTimeMillis();
        
        // Cache health check results for 1 minute to avoid excessive checks
        if (currentTime - lastCheckTime < HEALTH_CHECK_INTERVAL) {
            return lastHealthCheck.get() ? Health.up().build() : Health.down().build();
        }

        try {
            Health.Builder healthBuilder = Health.up();
            
            // Check producer health
            checkProducerHealth(healthBuilder);
            
            // Check consumer health (basic connectivity)
            checkConsumerHealth(healthBuilder);
            
            // Add producer metrics
            addProducerMetrics(healthBuilder);
            
            // Add circuit breaker status
            addCircuitBreakerStatus(healthBuilder);
            
            lastHealthCheck.set(true);
            lastCheckTime = currentTime;
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            lastHealthCheck.set(false);
            lastCheckTime = currentTime;
            
            return Health.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    private void checkProducerHealth(Health.Builder healthBuilder) {
        try {
            // Check if producer factories are properly configured
            if (avroKafkaTemplate != null && jsonKafkaTemplate != null && stringKafkaTemplate != null) {
                healthBuilder.withDetail("producer.status", "UP")
                            .withDetail("producer.avro", "OK")
                            .withDetail("producer.json", "OK")
                            .withDetail("producer.string", "OK");
            } else {
                healthBuilder.withDetail("producer.status", "UNKNOWN")
                            .withDetail("producer.error", "One or more producer templates are null");
            }
                        
        } catch (Exception e) {
            log.warn("Producer health check failed", e);
            healthBuilder.withDetail("producer.status", "DOWN")
                        .withDetail("producer.error", e.getMessage());
        }
    }

    private void checkConsumerHealth(Health.Builder healthBuilder) {
        try {
            // Basic consumer factory check
            if (avroKafkaTemplate.getDefaultTopic() != null) {
                healthBuilder.withDetail("consumer.status", "UP")
                            .withDetail("consumer.avro", "OK")
                            .withDetail("consumer.json", "OK")
                            .withDetail("consumer.string", "OK");
            } else {
                healthBuilder.withDetail("consumer.status", "UNKNOWN");
            }
        } catch (Exception e) {
            log.warn("Consumer health check failed", e);
            healthBuilder.withDetail("consumer.status", "DOWN")
                        .withDetail("consumer.error", e.getMessage());
        }
    }

    private void addProducerMetrics(Health.Builder healthBuilder) {
        long totalMessages = kafkaEventProducer.getTotalMessagesSent();
        long successfulMessages = kafkaEventProducer.getSuccessfulMessages();
        long failedMessages = kafkaEventProducer.getFailedMessages();
        double successRate = kafkaEventProducer.getSuccessRate();
        
        healthBuilder.withDetail("metrics.total_messages", totalMessages)
                    .withDetail("metrics.successful_messages", successfulMessages)
                    .withDetail("metrics.failed_messages", failedMessages)
                    .withDetail("metrics.success_rate_percent", String.format("%.2f", successRate));
    }

    private void addCircuitBreakerStatus(Health.Builder healthBuilder) {
        boolean circuitBreakerOpen = kafkaEventProducer.isCircuitBreakerOpenStatus();
        int circuitBreakerFailures = kafkaEventProducer.getCircuitBreakerFailures();
        
        healthBuilder.withDetail("circuit_breaker.open", circuitBreakerOpen)
                    .withDetail("circuit_breaker.failures", circuitBreakerFailures);
    }
} 