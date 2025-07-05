package com.example.demo.kafka.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaConfigProperties {
    
    private String bootstrapServers;
    private Producer producer = new Producer();
    private Consumer consumer = new Consumer();
    
    @Data
    public static class Producer {
        private String keySerializer;
        private String valueSerializer;
        private ProducerProperties properties = new ProducerProperties();
        private String acks;
        private Integer retries;
        private Integer lingerMs;
    }
    
    @Data
    public static class Consumer {
        private String groupId;
        private String autoOffsetReset;
        private String keyDeserializer;
        private String valueDeserializer;
        private ConsumerProperties properties = new ConsumerProperties();
        private Boolean enableAutoCommit;
    }
    
    @Data
    public static class ProducerProperties {
        private String schemaRegistryUrl;
    }
    
    @Data
    public static class ConsumerProperties {
        private String schemaRegistryUrl;
    }
} 