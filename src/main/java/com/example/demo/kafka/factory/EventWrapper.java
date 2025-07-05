package com.example.demo.kafka.factory;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EventWrapper<T> {
 
    private String eventId = UUID.randomUUID().toString();   // Unique ID for idempotency
    private String eventType;         // CREATE / UPDATE / DELETE / etc.
    private String entityType;        // Logical entity name (e.g. User, Order)
    private String source;            // Originating service (e.g. user-service)
    private String topic;             // Kafka topic (optional, for logging)
    private String version;           // Schema version (e.g. v1, v2)

    private String traceId;           // Correlation ID for observability
    private String producerRegion;    // Optional: region name

    private Integer retryCount;       // Retry attempt count, if applicable
    private Integer ttl;              // Optional: TTL in seconds or ms

    private OffsetDateTime timestamp = OffsetDateTime.now(); // Creation timestamp

    private T data;                   // The actual business payload

}
