package com.dbtraining.reconx.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(
            KafkaTemplate<Object, Object> template
    ) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        template,
                        (ConsumerRecord<?, ?> record, Exception exception) ->
                                new TopicPartition(
                                        record.topic() + "-dlq",
                                        record.partition()
                                )
                );

        ExponentialBackOff backoff =
                new ExponentialBackOff(1000L, 2.0);

        backoff.setMaxAttempts(3);

        return new DefaultErrorHandler(recoverer, backoff);
    }
}