package com.example.demo.kafka.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.error-handling")
public class ErrorHandlingConfig {
    
    private Retry retry = new Retry();
    private DeadLetterQueue dlt = new DeadLetterQueue();
    private Monitoring monitoring = new Monitoring();
    
    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long initialDelayMs = 1000;
        private double multiplier = 2.0;
        private long maxDelayMs = 10000;
        private List<String> nonRetryableExceptions = List.of(
            "java.lang.IllegalArgumentException",
            "java.lang.NullPointerException",
            "java.lang.UnsupportedOperationException"
        );
    }
    
    @Data
    public static class DeadLetterQueue {
        private String suffix = "-dlt";
        private boolean enabled = true;
        private boolean includeHeaders = true;
        private boolean includeStackTrace = false;
        private int maxMessageSize = 1048576; // 1MB
    }
    
    @Data
    public static class Monitoring {
        private boolean enabled = true;
        private String metricsPrefix = "kafka.error";
        private boolean logRetryAttempts = true;
        private boolean logDltMessages = true;
        private List<String> alertEmails = List.of();
    }
} 