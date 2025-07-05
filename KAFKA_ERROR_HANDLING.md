# Kafka Error Handling Implementation

This document explains the comprehensive error handling implementation for your Kafka application, including retry logic, dead letter queues, and monitoring.

## Overview

The error handling system provides:
- **Automatic Retry Logic**: Configurable retry attempts with exponential backoff
- **Dead Letter Queue (DLT)**: Failed messages are sent to DLT topics for analysis
- **Error Classification**: Different handling for different types of errors
- **Monitoring & Alerting**: Comprehensive logging and metrics
- **Configurable Behavior**: All settings can be customized via application.yaml

## Components

### 1. KafkaErrorHandler
The main error handler that implements `CommonErrorHandler`:

```java
@Component
public class KafkaErrorHandler implements CommonErrorHandler {
    // Handles individual message errors
    // Implements retry logic
    // Sends failed messages to DLT
}
```

**Features:**
- Tracks retry attempts per message
- Implements exponential backoff
- Classifies errors (retryable vs non-retryable)
- Sends failed messages to DLT topics

### 2. DeadLetterMessage
Model for DLT messages with error context:

```java
@Data
@Builder
public class DeadLetterMessage {
    private String originalTopic;
    private String originalKey;
    private Object originalValue;
    private Headers originalHeaders;
    private String errorMessage;
    private String errorClass;
    private int retryAttempts;
    private long timestamp;
    // ... additional metadata
}
```

### 3. DltConsumer
Handles messages in dead letter queues:

```java
@Component
public class DltConsumer {
    @KafkaListener(topics = "#{'${kafka.topics.error-handling.dlt:}'.split(',')}")
    public void consumeDltMessage(DeadLetterMessage dltMessage) {
        // Process failed messages
        // Send alerts
        // Store for analysis
    }
}
```

## Configuration

### application.yaml

```yaml
kafka:
  error-handling:
    retry:
      max-attempts: 3                    # Maximum retry attempts
      initial-delay-ms: 1000             # Initial delay between retries
      multiplier: 2.0                    # Exponential backoff multiplier
      max-delay-ms: 10000                # Maximum delay between retries
      non-retryable-exceptions:          # Exceptions that should not be retried
        - java.lang.IllegalArgumentException
        - java.lang.NullPointerException
        - java.lang.UnsupportedOperationException
    dlt:
      suffix: "-dlt"                     # DLT topic suffix
      enabled: true                      # Enable/disable DLT
      include-headers: true              # Include original headers in DLT
      include-stack-trace: false         # Include stack trace in DLT
      max-message-size: 1048576          # Maximum DLT message size (1MB)
    monitoring:
      enabled: true                      # Enable monitoring
      metrics-prefix: "kafka.error"      # Metrics prefix
      log-retry-attempts: true           # Log retry attempts
      log-dlt-messages: true             # Log DLT messages
      alert-emails: []                   # Email addresses for alerts
```

## Error Handling Flow

### 1. Message Processing Error
```
Message → Consumer → Error → KafkaErrorHandler
```

### 2. Retry Decision
```
Error → Check if retryable → Check retry count → Decide action
```

**Retryable Conditions:**
- Exception is not in `non-retryable-exceptions` list
- Retry count < `max-attempts`
- Not a fatal exception

### 3. Retry Logic
```
Retry → Pause Consumer → Wait (exponential backoff) → Resume Consumer
```

**Exponential Backoff:**
- Attempt 1: 1000ms delay
- Attempt 2: 2000ms delay  
- Attempt 3: 4000ms delay
- Maximum: 10000ms delay

### 4. Dead Letter Queue
```
Max Retries Exceeded → Create DLT Message → Send to DLT Topic → DltConsumer
```

## Error Types

### Retryable Errors
- Network timeouts
- Database connection issues
- Temporary service unavailability
- Rate limiting

### Non-Retryable Errors
- `IllegalArgumentException` (invalid data)
- `NullPointerException` (missing data)
- `UnsupportedOperationException` (unsupported operation)

### Fatal Errors
- `SecurityException` (security issues)
- `OutOfMemoryError` (system issues)
- `StackOverflowError` (system issues)

## DLT Topics

DLT topics are automatically created by appending the configured suffix:

```
Original Topic: user-event
DLT Topic: user-event-dlt

Original Topic: education-json-topic  
DLT Topic: education-json-topic-dlt
```

## Monitoring & Alerting

### Logging
- Retry attempts are logged with attempt count
- DLT messages are logged with error details
- Error statistics are available via `getRetryStatistics()`

### Metrics
- Active retry attempts count
- DLT message count
- Error rates by exception type

### Alerts
- Configure email alerts for DLT messages
- Different handling for different error types
- Integration with monitoring systems

## Usage Examples

### 1. Custom Error Handling
```java
@Component
public class CustomEventHandler {
    
    public void handleEvent(EventWrapper<SpecificRecord> event) {
        try {
            // Process event
            processEvent(event);
        } catch (IllegalArgumentException e) {
            // This will be sent directly to DLT (non-retryable)
            throw e;
        } catch (DatabaseException e) {
            // This will be retried (retryable)
            throw e;
        }
    }
}
```

### 2. DLT Message Analysis
```java
@Component
public class DltAnalyzer {
    
    public void analyzeDltMessage(DeadLetterMessage dltMessage) {
        log.info("Analyzing DLT message: {}", dltMessage);
        
        // Check error patterns
        if (dltMessage.getRetryAttempts() >= 3) {
            log.warn("Message failed after max retries: {}", dltMessage.getOriginalKey());
        }
        
        // Send alerts based on error type
        if ("DatabaseException".equals(dltMessage.getErrorClass())) {
            sendDatabaseAlert(dltMessage);
        }
    }
}
```

### 3. Error Statistics
```java
@RestController
public class ErrorController {
    
    private final KafkaErrorHandler errorHandler;
    
    @GetMapping("/kafka/error-stats")
    public Map<String, Object> getErrorStats() {
        return errorHandler.getRetryStatistics();
    }
}
```

## Best Practices

### 1. Error Classification
- Clearly classify exceptions as retryable vs non-retryable
- Use specific exception types for different error scenarios
- Avoid catching generic `Exception` in business logic

### 2. DLT Management
- Monitor DLT topics regularly
- Implement DLT message analysis
- Set up alerts for DLT message volume
- Archive old DLT messages

### 3. Configuration
- Start with conservative retry settings
- Monitor error rates and adjust configuration
- Use different settings for different environments
- Document error handling decisions

### 4. Monitoring
- Set up dashboards for error rates
- Configure alerts for high error rates
- Monitor DLT topic sizes
- Track retry attempt patterns

## Troubleshooting

### Common Issues

1. **High DLT Volume**
   - Check if errors are properly classified
   - Review retry configuration
   - Investigate root cause of errors

2. **Infinite Retry Loops**
   - Verify non-retryable exception configuration
   - Check for logic errors in handlers
   - Review error classification

3. **Performance Issues**
   - Monitor retry attempt tracking
   - Check DLT message sizes
   - Review exponential backoff settings

### Debug Commands

```bash
# Check DLT topic messages
kafka-console-consumer --bootstrap-server localhost:9092 --topic user-event-dlt --from-beginning

# Monitor error rates
curl http://localhost:8080/kafka/error-stats

# Check application logs
tail -f application.log | grep "KafkaErrorHandler"
``` 