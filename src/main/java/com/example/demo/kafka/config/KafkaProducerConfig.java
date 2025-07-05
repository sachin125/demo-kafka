package com.example.demo.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import org.springframework.kafka.support.serializer.JsonSerializer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.producer.retries:3}")
    private int retries;

    @Value("${spring.kafka.producer.request.timeout.ms:30000}")
    private int requestTimeoutMs;

    @Value("${spring.kafka.properties.auto.create.topics.enable:true}")
    private boolean autoCreateTopics;

    @Value("${spring.kafka.properties.metadata.max.age.ms:30000}")
    private int metadataMaxAgeMs;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.linger.ms:5}")
    private int lingerMs;

    @Value("${spring.kafka.producer.batch.size:16384}")
    private int batchSize;

    @Value("${spring.kafka.producer.buffer.memory:33554432}")
    private long bufferMemory;

    @Value("${spring.kafka.producer.compression.type:snappy}")
    private String compressionType;

    @Value("${spring.kafka.producer.max.in.flight.requests.per.connection:5}")
    private int maxInFlightRequests;

    @Value("${spring.kafka.producer.enable.idempotence:true}")
    private boolean enableIdempotence;

    private Map<String, Object> baseProducerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        configProps.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, metadataMaxAgeMs);
        configProps.put("auto.create.topics.enable", autoCreateTopics);
        
        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxInFlightRequests);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        
        // Additional best practices
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000); // 1 minute
        // Only set transactional ID if idempotence is enabled
        if (enableIdempotence) {
            configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "producer-" + System.currentTimeMillis());
        }
        
        return configProps;
    }

    // Avro ProducerFactory & KafkaTemplate
    @Bean
    public ProducerFactory<String, Object> avroProducerFactory() {
        Map<String, Object> configProps = baseProducerConfig();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configProps.put("schema.registry.url", schemaRegistryUrl);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean(name = "avroKafkaTemplate")
    public KafkaTemplate<String, Object> avroKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(avroProducerFactory());
        template.setObservationEnabled(true); // Enable Micrometer metrics
        return template;
    }
    

    // JSON ProducerFactory & KafkaTemplate
    @Bean
    public ProducerFactory<String, Object> jsonProducerFactory() {
        Map<String, Object> configProps = baseProducerConfig();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean(name = "jsonKafkaTemplate")
    public KafkaTemplate<String, Object> jsonKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(jsonProducerFactory());
        template.setObservationEnabled(true); // Enable Micrometer metrics
        return template;
    }

    // String ProducerFactory & KafkaTemplate
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> configProps = baseProducerConfig();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean(name = "stringKafkaTemplate")
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(stringProducerFactory());
        template.setObservationEnabled(true); // Enable Micrometer metrics
        return template;
    }
}
