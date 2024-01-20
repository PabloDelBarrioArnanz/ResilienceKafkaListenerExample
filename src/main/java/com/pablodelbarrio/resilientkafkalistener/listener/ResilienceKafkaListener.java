package com.pablodelbarrio.resilientkafkalistener.listener;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;


@Slf4j
@Component
public class ResilienceKafkaListener {

    /*
        Annotation config for the resilient listener
            - kafkaTemplate -> bean name for a kafka template will be used to send non-successfully events to the reprocess topics
            - attempts -> number of attempts for process a successfully an event
            - backoff -> delay set after between attempts, with the multiplier as much error with the event more delay will be applied
            - topicSuffixingStrategy -> it's the strategy for error topics naming, by default uses the delay value,
              but SUFFIX_WITH_INDEX_VALUE will set the attempt number
            - dltTopicSuffix -> similar than the past one but for the dead letter topic
    */
    @RetryableTopic(kafkaTemplate = "kafkaTemplate",
            attempts = "4",
            backoff = @Backoff(delay = 3000, multiplier = 1.5, maxDelay = 15000),
            autoCreateTopics = "false",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = "-dead-letter"
    )
    // Default listener config, only ensure to disable the auto commit in the containerFactory or in SpringProperties
    @KafkaListener(
            groupId = "resilience-kafka-listener",
            topics = "origin-topic",
            containerFactory = "kafkaResilienceListenerContainerFactory")
    public void listener(
            @Header(RECEIVED_TOPIC) String receivedTopic, // Topic name, only needed to check the attempt number
            @Header(value = DEFAULT_HEADER_ATTEMPTS, required = false, defaultValue = "1") Integer attempt,
            @Header(value = "HEADER_CUSTOM_ERROR", required = false) String error,
            ConsumerRecord<String, String> recordMessage,
            Acknowledgment ack) { // The acknowledgment needed for commit an event and avoid the retry process
        log.info("Received an event with content {} in topic {} with attempt {} with error {}",
                recordMessage.value(), receivedTopic, attempt, error);

        if (attempt.equals(1)) {
            log.info("Processing message for first time");
        } else if (attempt.equals(2)) {
            log.info("Processing message for second time");
        } else if (attempt.equals(3)) {
            log.info("Processing message for third time");
        } else {
            log.info("Processing message for forth & last time");
        }

        if ("OK".equalsIgnoreCase(recordMessage.value())) {
            ack.acknowledge(); // committing the event
        } else {
            /*
                When an exception it's thrown in the processing,
                the Retryable process will resend the event to the corresponding retry-topic determinate with the attempts done
            */
            throw new RuntimeException("Fail reading message");
        }
    }

    /*
        After all attempts are consumed, this function will listen the failed event in the dead-letter queue.
        Add desire functionality for non-successfully processed events to this function,
        but set the ack or all events in the dead-letter events will reprocess in each boot.

        This function must be placed in the same class as the listener.
    */
    @DltHandler
    public void deadLetterProcessor(@Header(KafkaHeaders.EXCEPTION_MESSAGE) String error, // Message from the exception witch made the event fail
                                    String message,
                                    Acknowledgment ack) {
        log.error("DltHandler processing with content {} with error {}", message, error);
        ack.acknowledge();
    }

}
