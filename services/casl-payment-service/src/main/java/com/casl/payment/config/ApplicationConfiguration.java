package com.casl.payment.config;

import com.casl.payment.domain.port.PaymentProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	PaymentProcessor paymentProcessor() {
		return new SimulatedPaymentProcessor();
	}
}
