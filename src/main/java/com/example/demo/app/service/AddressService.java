package com.example.demo.app.service;

import com.example.demo.app.repository.AddressRepository;
import com.example.demo.app.entity.Address;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {
    @Autowired
    private AddressRepository addressRepository;

    public Address createAddress(Address address) {
        return addressRepository.save(address);
    }

    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }

    public Optional<Address> getAddressById(Long id) {
        return addressRepository.findById(id);
    }

    public Address updateAddress(Long id, Address addressDetails) {
        return addressRepository.findById(id)
                .map(address -> {
                    address.setStreet(addressDetails.getStreet());
                    address.setCity(addressDetails.getCity());
                    address.setState(addressDetails.getState());
                    address.setZip(addressDetails.getZip());
                    address.setUser(addressDetails.getUser());
                    return addressRepository.save(address);
                })
                .orElseGet(() -> {
                    addressDetails.setId(id);
                    return addressRepository.save(addressDetails);
                });
    }

    public void deleteAddress(Long id) {
        addressRepository.deleteById(id);
    }
} 