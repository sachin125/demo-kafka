package com.example.demo.kafka.consumer.handler;

import org.springframework.stereotype.Component;

import com.example.avro.AvroUser;
import com.example.avro.mapper.AvroUserMapper;
import com.example.demo.app.entity.User;
import com.example.demo.kafka.consumer.handler.generic.SimpleKafkaEventHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//@EventHandler(entityType = "user", operation = "CREATE/ UPDATE/ DELETE")

@RequiredArgsConstructor
@Slf4j
@Component
public class UserEventHandler extends SimpleKafkaEventHandler<User, AvroUser>{

    private final AvroUserMapper mapper;
    
    @Override
    public Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    public Class<AvroUser> getPayloadClass() {
        return AvroUser.class;
    }
    

    // @KafkaListener(topics = {"user-event-create", "user-event-update", "user-event-delete"}, 
    // groupId = "user-consumer-group", 
    // containerFactory = "kafkaListenerContainerFactory")

    @Override
    public void handleCreate(User user) {
        log.info("Handling User CREATE: ID={}, Name={}, Email={}",
                    user.getId(), user.getName(), user.getEmail());
        // Add custom logic (e.g., save to downstream system)
        log.info("Successfully processed user creation for: {}", user.getName());
    }

    @Override
    public void handleUpdate(User user) {
        log.info("Handling User UPDATE: ID={}, Name={}, Email={}",
                    user.getId(), user.getName(), user.getEmail());
        // Add custom logic
    }

    @Override
    public void handleDelete(User user) {
        log.info("Handling User DELETE: ID={}", user.getId());
        // Add custom logic
    }

    @Override
    protected User toEntity(AvroUser avroUser) {
        return mapper.toEntity(avroUser);
    }

}