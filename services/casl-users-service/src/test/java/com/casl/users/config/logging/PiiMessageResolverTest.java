package com.casl.users.config.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverFactory;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios del resolver/factory JSON de PII sin arrancar Spring: validan el
 * nombre del resolver {@code piiMessage} y la delegación en {@link PiiMasker}.
 */
class PiiMessageResolverTest {

	private final PiiMessageResolverFactory factory = PiiMessageResolverFactory.getInstance();

	@Test
	void factoryExposesPiiMessageResolverNameAndCreatesEventResolver() {
		assertThat(factory.getName()).isEqualTo("piiMessage");
		assertThat(factory.create(null, null)).isInstanceOf(EventResolver.class);
	}

	@Test
	void factoryIsAnEventResolverFactory() {
		assertThat(PiiMessageResolverFactory.getInstance()).isInstanceOf(EventResolverFactory.class);
	}

	@Test
	void resolveWritesMaskedMessage() {
		LogEvent logEvent = Log4jLogEvent.newBuilder()
				.setLevel(Level.INFO)
				.setMessage(new SimpleMessage("User created email=ana@example.com"))
				.build();

		String json = resolve(logEvent);

		assertThat(json).contains("***");
		assertThat(json).doesNotContain("ana@example.com");
	}

	@Test
	void resolveWritesPlainMessageWhenNoPii() {
		LogEvent logEvent = Log4jLogEvent.newBuilder()
				.setLevel(Level.INFO)
				.setMessage(new SimpleMessage("Order id=3f1c2a7e status=CREATED"))
				.build();

		String json = resolve(logEvent);

		assertThat(json).contains("Order id=3f1c2a7e status=CREATED");
	}

	@Test
	void resolveWritesEmptyStringWhenMessageIsNull() {
		LogEvent logEvent = Log4jLogEvent.newBuilder()
				.setLevel(Level.INFO)
				.setMessage(null)
				.build();

		String json = resolve(logEvent);

		assertThat(json).isEqualTo("\"\"");
	}

	private String resolve(LogEvent logEvent) {
		JsonWriter jsonWriter = JsonWriter.newBuilder()
				.setMaxStringLength(1024)
				.setTruncatedStringSuffix("")
				.build();
		new PiiMessageResolver().resolve(logEvent, jsonWriter);
		return jsonWriter.getStringBuilder().toString();
	}
}
