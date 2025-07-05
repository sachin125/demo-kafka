package com.example.avro.mapper;

import com.example.demo.app.entity.User;
import com.example.avro.AvroAddress;
import com.example.avro.AvroUser;
import com.example.demo.app.entity.Address;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvroUserMapper implements AvroBaseMapper<User, AvroUser> {

    @Autowired
    private AvroAddressMapper addressMapper;

    public AvroUser toAvro(User user) {
        if (user == null) return null;
        
        return AvroUser.newBuilder()
                .setId(user.getId())
                .setName(user.getName() != null ? user.getName() : "")
                .setEmail(user.getEmail())
                .setAge(user.getAge())
                .setAvroAddresses(toAvro(user.getAddresses()))
                .build();
    }

    public List<AvroAddress> toAvro(List<Address> addresses) {
        if (addresses == null) {
            return null;
        }
        return addresses.stream()
                .map(address -> addressMapper.toAvro(address))
                .collect(Collectors.toList());
    }
    
    public User toEntity(AvroUser avro) {
        if (avro == null) return null;
        
        User user = new User();
        user.setId(avro.getId());
        user.setName(avro.getName() != null ? avro.getName().toString() : null);
        user.setEmail(avro.getEmail() != null ? avro.getEmail().toString() : null);
        user.setAge(avro.getAge());
    
        if (avro.getAvroAddresses() != null) {
            List<Address> addresses = toEntity(avro.getAvroAddresses());
            user.setAddresses(addresses);
        }
    
        return user;
    }
    
    public List<Address> toEntity(List<AvroAddress> avroAddresses) {
        if (avroAddresses == null) {
            return null;
        }
        return avroAddresses.stream()
                .map(avroAddress -> addressMapper.toEntity(avroAddress))
                .collect(Collectors.toList());
    }
}
