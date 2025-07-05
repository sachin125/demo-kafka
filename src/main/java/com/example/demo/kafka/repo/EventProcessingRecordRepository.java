package com.example.demo.kafka.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.kafka.entity.EventProcessingRecord;

public interface EventProcessingRecordRepository extends JpaRepository<EventProcessingRecord, String> {

}
