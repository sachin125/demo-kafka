package com.example.demo.kafka.producer.publisher.generic;

public interface IGenericEventPublisher<Entity> {
    
    void publishCreate(Entity entity);
    
    void publishUpdate(Entity entity);
    
    void publishDelete(Entity entity);

}

