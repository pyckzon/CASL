package com.casl.order.config;

import com.casl.order.domain.port.OrderRepository;
import com.casl.order.domain.port.PaymentGateway;
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
	PaymentGateway paymentGateway() {
		return new SimulatedPaymentGateway();
	}
}
