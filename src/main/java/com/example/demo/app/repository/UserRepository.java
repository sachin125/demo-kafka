package com.example.demo.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.app.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
} 