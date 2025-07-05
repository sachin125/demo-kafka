package com.example.demo.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicsConfig {
    
    private ProducerTopics producer = new ProducerTopics();
    private ConsumerTopics consumer = new ConsumerTopics();
    
    // Legacy support (deprecated)
    private List<String> entities;
    private List<String> json;
    private List<String> string;
    
    @Data
    public static class ProducerTopics {
        private List<String> entities;
        private List<String> json;
        private List<String> string;
    }
    
    @Data
    public static class ConsumerTopics {
        private List<String> entities;
        private List<String> json;
        private List<String> string;
    }
} 