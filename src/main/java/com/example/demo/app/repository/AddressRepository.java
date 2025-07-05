package com.example.demo.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
} 