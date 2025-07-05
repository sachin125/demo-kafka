package com.example.demo.kafka.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.kafka.common.header.Headers;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetterMessage {
    
    // Original message information
    private String originalTopic;
    private String originalKey;
    private Object originalValue;
    private Headers originalHeaders;
    
    // Error information
    private String errorMessage;
    private String errorClass;
    private String errorStackTrace;
    
    // Processing information
    private int retryAttempts;
    private long timestamp;
    private String consumerGroupId;
    private String consumerId;
    
    // Additional context
    private Map<String, Object> additionalContext;
    
    // Message metadata
    private long originalOffset;
    private int originalPartition;
    private long originalTimestamp;
    
    public static DeadLetterMessageBuilder builder() {
        return new DeadLetterMessageBuilder();
    }
    
    public static class DeadLetterMessageBuilder {
        private String originalTopic;
        private String originalKey;
        private Object originalValue;
        private Headers originalHeaders;
        private String errorMessage;
        private String errorClass;
        private String errorStackTrace;
        private int retryAttempts;
        private long timestamp = Instant.now().toEpochMilli();
        private String consumerGroupId;
        private String consumerId;
        private Map<String, Object> additionalContext = new HashMap<>();
        private long originalOffset;
        private int originalPartition;
        private long originalTimestamp;
        
        public DeadLetterMessageBuilder originalTopic(String originalTopic) {
            this.originalTopic = originalTopic;
            return this;
        }
        
        public DeadLetterMessageBuilder originalKey(String originalKey) {
            this.originalKey = originalKey;
            return this;
        }
        
        public DeadLetterMessageBuilder originalValue(Object originalValue) {
            this.originalValue = originalValue;
            return this;
        }
        
        public DeadLetterMessageBuilder originalHeaders(Headers originalHeaders) {
            this.originalHeaders = originalHeaders;
            return this;
        }
        
        public DeadLetterMessageBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public DeadLetterMessageBuilder errorClass(String errorClass) {
            this.errorClass = errorClass;
            return this;
        }
        
        public DeadLetterMessageBuilder errorStackTrace(String errorStackTrace) {
            this.errorStackTrace = errorStackTrace;
            return this;
        }
        
        public DeadLetterMessageBuilder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }
        
        public DeadLetterMessageBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public DeadLetterMessageBuilder consumerGroupId(String consumerGroupId) {
            this.consumerGroupId = consumerGroupId;
            return this;
        }
        
        public DeadLetterMessageBuilder consumerId(String consumerId) {
            this.consumerId = consumerId;
            return this;
        }
        
        public DeadLetterMessageBuilder additionalContext(Map<String, Object> additionalContext) {
            this.additionalContext = additionalContext;
            return this;
        }
        
        public DeadLetterMessageBuilder addContext(String key, Object value) {
            this.additionalContext.put(key, value);
            return this;
        }
        
        public DeadLetterMessageBuilder originalOffset(long originalOffset) {
            this.originalOffset = originalOffset;
            return this;
        }
        
        public DeadLetterMessageBuilder originalPartition(int originalPartition) {
            this.originalPartition = originalPartition;
            return this;
        }
        
        public DeadLetterMessageBuilder originalTimestamp(long originalTimestamp) {
            this.originalTimestamp = originalTimestamp;
            return this;
        }
        
        public DeadLetterMessage build() {
            DeadLetterMessage message = new DeadLetterMessage();
            message.setOriginalTopic(originalTopic);
            message.setOriginalKey(originalKey);
            message.setOriginalValue(originalValue);
            message.setOriginalHeaders(originalHeaders);
            message.setErrorMessage(errorMessage);
            message.setErrorClass(errorClass);
            message.setErrorStackTrace(errorStackTrace);
            message.setRetryAttempts(retryAttempts);
            message.setTimestamp(timestamp);
            message.setConsumerGroupId(consumerGroupId);
            message.setConsumerId(consumerId);
            message.setAdditionalContext(additionalContext);
            message.setOriginalOffset(originalOffset);
            message.setOriginalPartition(originalPartition);
            message.setOriginalTimestamp(originalTimestamp);
            return message;
        }
    }
} 