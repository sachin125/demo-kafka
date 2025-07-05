// package com.example.demo.kafka.consumer;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.kafka.support.Acknowledgment;
// import org.springframework.stereotype.Component;

// import com.example.demo.kafka.model.DeadLetterMessage;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class DltConsumer {

//     @KafkaListener(
//         topics = "#{'${kafka.topics.error-handling.dlt:}'.split(',')}",
//         groupId = "${kafka.consumer.group-id:my-group}-dlt",
//         containerFactory = "jsonKafkaListenerContainerFactory"
//     )
//     public void consumeDltMessage(DeadLetterMessage dltMessage, Acknowledgment acknowledgment) {
//         try {
//             log.warn("Processing DLT message: OriginalTopic={}, OriginalKey={}, ErrorClass={}, RetryAttempts={}", 
//                     dltMessage.getOriginalTopic(), 
//                     dltMessage.getOriginalKey(), 
//                     dltMessage.getErrorClass(), 
//                     dltMessage.getRetryAttempts());
            
//             // Log the error details
//             log.error("DLT Error Details: Message={}, ErrorClass={}, ErrorMessage={}", 
//                     dltMessage.getOriginalValue(), 
//                     dltMessage.getErrorClass(), 
//                     dltMessage.getErrorMessage());
            
//             // Here you can implement specific DLT handling logic:
//             // 1. Send notifications to administrators
//             // 2. Store in database for analysis
//             // 3. Trigger manual intervention workflows
//             // 4. Send to monitoring systems
            
//             handleDltMessage(dltMessage);
            
//             acknowledgment.acknowledge();
            
//         } catch (Exception e) {
//             log.error("Error processing DLT message: {}", dltMessage, e);
//             // For DLT, we might want to acknowledge even on error to avoid infinite loops
//             acknowledgment.acknowledge();
//         }
//     }
    
//     private void handleDltMessage(DeadLetterMessage dltMessage) {
//         // Example DLT handling logic
//         switch (dltMessage.getErrorClass()) {
//             case "IllegalArgumentException":
//                 handleValidationError(dltMessage);
//                 break;
//             case "NullPointerException":
//                 handleNullPointerError(dltMessage);
//                 break;
//             case "DatabaseException":
//                 handleDatabaseError(dltMessage);
//                 break;
//             default:
//                 handleGenericError(dltMessage);
//         }
//     }
    
//     private void handleValidationError(DeadLetterMessage dltMessage) {
//         log.warn("Validation error in DLT: Topic={}, Key={}, Error={}", 
//                 dltMessage.getOriginalTopic(), 
//                 dltMessage.getOriginalKey(), 
//                 dltMessage.getErrorMessage());
//         // Send notification to data quality team
//         // Store in validation error database
//     }
    
//     private void handleNullPointerError(DeadLetterMessage dltMessage) {
//         log.error("Null pointer error in DLT: Topic={}, Key={}", 
//                 dltMessage.getOriginalTopic(), 
//                 dltMessage.getOriginalKey());
//         // Send alert to development team
//         // Create bug report
//     }
    
//     private void handleDatabaseError(DeadLetterMessage dltMessage) {
//         log.error("Database error in DLT: Topic={}, Key={}, Error={}", 
//                 dltMessage.getOriginalTopic(), 
//                 dltMessage.getOriginalKey(), 
//                 dltMessage.getErrorMessage());
//         // Send alert to database team
//         // Check database connectivity
//     }
    
//     private void handleGenericError(DeadLetterMessage dltMessage) {
//         log.error("Generic error in DLT: Topic={}, Key={}, ErrorClass={}, Error={}", 
//                 dltMessage.getOriginalTopic(), 
//                 dltMessage.getOriginalKey(), 
//                 dltMessage.getErrorClass(), 
//                 dltMessage.getErrorMessage());
//         // Send general alert
//         // Store for analysis
//     }
// } 