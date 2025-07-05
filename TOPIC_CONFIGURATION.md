# Kafka Topic Configuration

This document explains how to configure different topics for producers and consumers in your Kafka application.

## Overview

Your application can now be configured to:
- **Produce** events to specific topics (e.g., user and address events)
- **Consume** events from different topics (e.g., education events from other services)

## Configuration Structure

### application.yaml

```yaml
kafka:
  topics:
    # Producer topics - this app can produce to these topics
    producer:
      entities: user-event,address-event
      json: user-json-topic,address-json-topic
      string: user-string-topic,address-string-topic
    # Consumer topics - this app can consume from these topics
    consumer:
      entities: education-event-create,education-event-update,education-event-delete
      json: education-json-topic
      string: education-string-topic
```

## Use Cases

### Scenario 1: User Management Service
```yaml
kafka:
  topics:
    producer:
      entities: user-event,user-profile-event
    consumer:
      entities: address-event,education-event
```

### Scenario 2: Address Management Service
```yaml
kafka:
  topics:
    producer:
      entities: address-event
    consumer:
      entities: user-event,education-event
```

### Scenario 3: Education Management Service
```yaml
kafka:
  topics:
    producer:
      entities: education-event
    consumer:
      entities: user-event,address-event
```

## Implementation Details

### Producer Configuration
- Uses `kafka.topics.producer.*` configuration
- Publishes to topics defined in producer section
- Supports Avro, JSON, and String formats

### Consumer Configuration
- Uses `kafka.topics.consumer.*` configuration
- Listens to topics defined in consumer section
- Automatically registers handlers for consumed entity types

### Handler Registration
The `KafkaEventHandlerRegistry` automatically discovers and registers handlers:
- `UserEventHandler` → handles "user" events
- `AddressEventHandler` → handles "address" events  
- `EducationEventHandler` → handles "education" events

## Example: Your Current Setup

Your application is configured to:

**Produce:**
- User events to `user-event` topic
- Address events to `address-event` topic

**Consume:**
- Education events from `education-event-create`, `education-event-update`, `education-event-delete` topics

## Adding New Entity Types

1. **Create the handler:**
```java
@Component
public class NewEntityEventHandler extends SimpleKafkaEventHandler<NewEntity, AvroNewEntity> {
    // Implementation
}
```

2. **Update configuration:**
```yaml
kafka:
  topics:
    producer:
      entities: user-event,address-event,new-entity-event
    consumer:
      entities: education-event-create,education-event-update,education-event-delete,other-entity-event
```

3. **The handler will be automatically registered** and will handle events for the entity type matching its class name.

## Migration from Legacy Configuration

The old configuration is still supported for backward compatibility:
```yaml
kafka:
  topics:
    entities: user-event-create,user-event-update,user-event-delete
    json: user-json-topic
    string: user-string-topic
```

However, it's recommended to migrate to the new producer/consumer structure for better separation of concerns. 