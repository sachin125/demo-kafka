package com.example.demo.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final CommonErrorHandler kafkaErrorHandler;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:my-group}")
    private String groupId;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.properties.metadata.max.age.ms:30000}")
    private int metadataMaxAgeMs;

    @Value("${spring.kafka.consumer.properties.specific.avro.reader:true}")
    private boolean specificAvroReader;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:com.example.model}")
    private String trustedPackages;

    @Value("${spring.kafka.consumer.session.timeout.ms:30000}")
    private int sessionTimeoutMs;

    @Value("${spring.kafka.consumer.heartbeat.interval.ms:3000}")
    private int heartbeatIntervalMs;

    @Value("${spring.kafka.consumer.max.poll.records:500}")
    private int maxPollRecords;

    @Value("${spring.kafka.consumer.max.poll.interval.ms:300000}")
    private int maxPollIntervalMs;

    @Value("${spring.kafka.consumer.fetch.min.bytes:1}")
    private int fetchMinBytes;

    @Value("${spring.kafka.consumer.fetch.max.wait.ms:500}")
    private int fetchMaxWaitMs;

    @Value("${spring.kafka.consumer.enable.auto.commit:false}")
    private boolean enableAutoCommit;

    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    private Map<String, Object> baseConsumerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        configProps.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, metadataMaxAgeMs);
        
        // Performance and reliability settings
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        
        // Additional best practices
        configProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "consumer-" + System.currentTimeMillis());
        configProps.put(ConsumerConfig.CHECK_CRCS_CONFIG, true);
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        return configProps;
    }

    @Bean
    public ConsumerFactory<String, Object> avroConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfig();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configProps.put("schema.registry.url", schemaRegistryUrl);
        configProps.put("specific.avro.reader", specificAvroReader);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean(name = "avroKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> avroKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(avroConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        
        // Concurrency and performance settings
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setSyncCommits(true);
        factory.getContainerProperties().setIdleBetweenPolls(1000);
        
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfig();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean(name = "jsonKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        
        // Concurrency and performance settings
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setSyncCommits(true);
        factory.getContainerProperties().setIdleBetweenPolls(1000);
        
        return factory;
    }

    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> configProps = baseConsumerConfig();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(stringConsumerFactory());
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        
        // Concurrency and performance settings
        factory.setConcurrency(concurrency);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setSyncCommits(true);
        factory.getContainerProperties().setIdleBetweenPolls(1000);
        
        return factory;
    }
}