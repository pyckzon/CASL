package com.casl.notification.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

	@Bean
	public DefaultErrorHandler kafkaErrorHandler(
			KafkaTemplate<String, Object> kafkaTemplate,
			MeterRegistry meterRegistry,
			@Value("${spring.application.name}") String service) {
		Counter caslDltPublished = Counter.builder("casl.dlt.published")
				.description("Kafka records published to the Dead Letter Topic after exhausting retries")
				.tag("service", service)
				.register(meterRegistry);
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
		ConsumerRecordRecoverer recovererAdapter = (record, exception) -> {
			caslDltPublished.increment();
			recoverer.accept(record, exception);
		};
		// Retry 3 times with 1s backoff, then send to DLT
		return new DefaultErrorHandler(recovererAdapter, new FixedBackOff(1000L, 3));
	}
}
