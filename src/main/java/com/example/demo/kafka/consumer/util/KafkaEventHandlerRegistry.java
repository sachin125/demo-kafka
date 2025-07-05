package com.example.demo.kafka.consumer.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler;

@Component
public class KafkaEventHandlerRegistry {

    private final Map<String, SimpleKafkaEventHandler<?, ?>> handlerMap = new HashMap<>();

    public KafkaEventHandlerRegistry(List<SimpleKafkaEventHandler<?, ?>> handlers) {
        for (SimpleKafkaEventHandler<?, ?> handler : handlers) {
            String entityType = handler.getEntityClass().getSimpleName().toLowerCase();
            if (handlerMap.containsKey(entityType)) {
                throw new IllegalStateException("Duplicate handler for entityType: " + entityType);
            }
            handlerMap.put(entityType, handler);
            System.out.println("Registered handler for entityType: " + entityType + " -> " + handler.getClass().getSimpleName());
        }
    }

    public Optional<SimpleKafkaEventHandler<?, ?>> getHandler(String entityType) {
        System.out.println("Looking for handler for entityType: " + entityType + " (lowercase: " + entityType.toLowerCase() + ")");
        System.out.println("Available handlers: " + handlerMap.keySet());
        return Optional.ofNullable(handlerMap.get(entityType.toLowerCase()));
    }
}
