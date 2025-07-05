package com.example.demo.kafka.producer.publisher;

import org.springframework.stereotype.Component;

import com.example.avro.AvroUser;
import com.example.avro.mapper.AvroUserMapper;
import com.example.demo.app.entity.User;
import com.example.demo.kafka.config.KafkaEventFormat;
import com.example.demo.kafka.producer.KafkaEventProducer;
import com.example.demo.kafka.producer.publisher.generic.SimpleKafkaEventPublisher;


@Component
public class UserEventPublisher extends SimpleKafkaEventPublisher<User, AvroUser> {

    private final AvroUserMapper mapper;

    public UserEventPublisher(KafkaEventProducer kafkaEventProducer, AvroUserMapper mapper) {
        super(kafkaEventProducer);
        this.mapper = mapper;
    }

    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    protected String extractKey(User entity) {
        return entity.getId().toString();
    }

    @Override
    protected AvroUser toAvro(User entity) {
        return mapper.toAvro(entity);
    }

    // New: Overloaded methods for all formats
    public void publishCreate(User user, KafkaEventFormat format) {
        publish(user, "create", format);
    }

    public void publishUpdate(User user, KafkaEventFormat format) {
        publish(user, "update", format);
    }

    public void publishDelete(User user, KafkaEventFormat format) {
        publish(user, "delete", format);
    }

    @Override
    protected String getEventSource() {
       return "UserEventPublisher";
    }
}
