package com.pablodelbarrio.resilientkafkalistener.config;

import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.charset.StandardCharsets;
import java.util.List;

@EnableScheduling
@Configuration
public class CustomRetryTopicConfigurationSupport extends RetryTopicConfigurationSupport {

    @Override
    protected void configureCustomizers(CustomizersConfigurer customizersConfigurer) {
        customizersConfigurer.customizeDeadLetterPublishingRecoverer(dlpr ->
                dlpr.addHeadersFunction((consumerRecord, exp) -> new RecordHeaders(List.of(
                        new DeadLetterPublishingRecoverer.SingleRecordHeader("HEADER_CUSTOM_ERROR",
                                exp.getMessage().getBytes(StandardCharsets.UTF_8))))));
    }

}
