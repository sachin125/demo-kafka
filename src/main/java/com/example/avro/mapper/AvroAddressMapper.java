package com.example.avro.mapper;

import com.example.demo.app.entity.Address;
import com.example.avro.AvroAddress;

import org.springframework.stereotype.Component;

@Component
public class AvroAddressMapper implements AvroBaseMapper<Address, AvroAddress> {

    public AvroAddress toAvro(Address address) {
        if (address == null) return null;
        
        return AvroAddress.newBuilder()
                .setId(address.getId())
                .setStreet(address.getStreet() != null ? address.getStreet() : "")
                .setCity(address.getCity() != null ? address.getCity() : "")
                .setZip(address.getZip())
                .setUserId(address.getUser() != null ? address.getUser().getId() : null)
                .build();
    }
    
    public Address toEntity(AvroAddress avro) {
        if (avro == null) return null;
        
        Address address = new Address();
        address.setId(avro.getId());
        address.setStreet(avro.getStreet() != null ? avro.getStreet().toString() : null);
        address.setCity(avro.getCity() != null ? avro.getCity().toString() : null);
        address.setZip(avro.getZip() != null ? avro.getZip().toString() : null);
        // Note: userId from Avro is not mapped to Address entity
        // as Address has a User relationship instead
        // The user relationship should be set separately if needed
        
        return address;
    }
} 