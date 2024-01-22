package com.pablodelbarrio.resilientkafkalistener.config;

/*
    This class it's not necessary to implement, override function in this class will change the default behaviour.

    For example add custom headers
    Or inclusive edit the event before to resent it to be retried

*/

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@EnableScheduling
@Configuration
public class CustomRetryTopicConfigurationSupport extends RetryTopicConfigurationSupport {

    @Override
    protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
        customizersConfigurer.customizeDeadLetterPublishingRecoverer(dlpr -> {
            dlpr.setAppendOriginalHeaders(true);
            dlpr.setStripPreviousExceptionHeaders(false);
            dlpr.addHeadersFunction((consumerRecord, exp) -> new RecordHeaders(List.of(
                    new DeadLetterPublishingRecoverer.SingleRecordHeader("CUSTOM_HEADER",
                            "CUSTOM_VALUE".getBytes(StandardCharsets.UTF_8)))));
        });
    }

    @Override
    protected java.util.function.Consumer<DeadLetterPublishingRecovererFactory> configureDeadLetterPublishingContainerFactory() {
        return factory -> factory.setDeadLetterPublisherCreator(CustomDLPR::new);
    }

    static class CustomDLPR extends DeadLetterPublishingRecoverer {

        CustomDLPR(Function<ProducerRecord<?, ?>, KafkaOperations<?, ?>> templateResolver,
                   BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver) {
            super(templateResolver, destinationResolver);
        }

    }

}
