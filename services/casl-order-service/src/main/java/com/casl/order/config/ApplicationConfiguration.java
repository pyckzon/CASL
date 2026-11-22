package com.casl.order.config;

import com.casl.order.adapter.out.payment.RestPaymentGateway;
import com.casl.order.domain.port.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	PaymentGateway paymentGateway(RestClient.Builder builder,
			@Value("${casl.payment.base-url:http://localhost:8673}") String baseUrl,
			@Value("${casl.payment.simulated:false}") boolean simulated) {
		if (simulated) {
			return new SimulatedPaymentGateway();
		}
		return new RestPaymentGateway(builder, baseUrl);
	}
}
