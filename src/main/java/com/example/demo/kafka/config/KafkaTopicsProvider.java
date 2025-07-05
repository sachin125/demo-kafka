package com.example.demo.kafka.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("kafkaTopicsProvider")
public class KafkaTopicsProvider {

    @Value("${kafka.topics.consumer.avro:}")
    private String avroTopics;

    @Value("${kafka.topics.consumer.json:}")
    private String jsonTopics;

    @Value("${kafka.topics.consumer.string:}")
    private String stringTopics;

    private static final List<String> AVRO_EVENT_SUFFIXES = Arrays.asList(
        "-create-avro", 
        "-update-avro", 
        "-delete-avro"
    );

    private static final List<String> JSON_EVENT_SUFFIXES = Arrays.asList(
        "-create-json", 
        "-update-json", 
        "-delete-json"
    );

    private static final List<String> STRING_EVENT_SUFFIXES = Arrays.asList(
        "-create-string", 
        "-update-string", 
        "-delete-string"
    );

    private String[] generateTopicsFromEntities(String entities, List<String> suffixes) {
        if (entities == null || entities.trim().isEmpty()) {
            return new String[0];
        }
        
        String[] entityNames = Arrays.stream(entities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        
        List<String> allTopics = Arrays.stream(entityNames)
                .flatMap(entity -> suffixes.stream().map(suffix -> entity + suffix))
                .collect(Collectors.toList());
        
        String[] topics = allTopics.toArray(new String[0]);
        log.info("Entry @class KafkaTopicsProvider @method generateTopicsFromEntities entities: {} -> topics: {}", 
                Arrays.toString(entityNames), Arrays.toString(topics));
        return topics;
    }

    public String[] getAvroTopics() {
        String[] topics = generateTopicsFromEntities(avroTopics, AVRO_EVENT_SUFFIXES);
        log.info("Entry @class KafkaTopicsProvider @method getAvroTopics topics: {}", Arrays.toString(topics));
        return topics;
    }

    public String[] getJsonTopics() {
        String[] topics = generateTopicsFromEntities(jsonTopics, JSON_EVENT_SUFFIXES);
        log.info("Entry @class KafkaTopicsProvider @method getJsonTopics topics: {}", Arrays.toString(topics));
        return topics;
    }

    public String[] getStringTopics() {
        String[] topics = generateTopicsFromEntities(stringTopics, STRING_EVENT_SUFFIXES);
        log.info("Entry @class KafkaTopicsProvider @method getStringTopics topics: {}", Arrays.toString(topics));
        return topics;
    }
}