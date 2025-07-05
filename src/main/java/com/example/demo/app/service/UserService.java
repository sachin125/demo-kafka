package com.example.demo.app.service;

import com.example.demo.app.repository.UserRepository;
import com.example.demo.kafka.config.KafkaEventFormat;
import com.example.demo.kafka.producer.publisher.UserEventPublisher;

import lombok.RequiredArgsConstructor;

import com.example.demo.app.entity.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
 
    private final UserRepository userRepository;

    private final UserEventPublisher userSimplePublisher;

    @Transactional
    public User create(User user) {
        User saved = userRepository.save(user);
        userSimplePublisher.publishCreate(saved, KafkaEventFormat.AVRO);
        return saved;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User update(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setName(userDetails.getName());
                    user.setEmail(userDetails.getEmail());
                    user.setAge(userDetails.getAge());
                    user.setAddresses(userDetails.getAddresses());
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    userDetails.setId(id);
                    return userRepository.save(userDetails);
                });
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
} 