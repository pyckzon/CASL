package com.casl.security.userstatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CachingUserStatusVerifierTest {

	private Jwt jwt(String subject) {
		return Jwt.withTokenValue("token-value").header("alg", "none").subject(subject).build();
	}

	@Test
	void freshEntryIsReusedWithoutCallingDelegateAgain() {
		CaslUserStatusProperties properties = new CaslUserStatusProperties();
		properties.setCacheTtl(Duration.ofSeconds(60));
		properties.setStaleGrace(Duration.ofSeconds(300));
		StubDelegate delegate = new StubDelegate();
		CachingUserStatusVerifier verifier = new CachingUserStatusVerifier(delegate, properties, new MutableClock());

		UserStatus first = verifier.verify(jwt("sub-1"));
		UserStatus second = verifier.verify(jwt("sub-1"));

		assertThat(first).isEqualTo(UserStatus.ACTIVE);
		assertThat(second).isEqualTo(UserStatus.ACTIVE);
		assertThat(delegate.invocations.get()).isEqualTo(1);
	}

	@Test
	void staleEntryWithinGraceIsReusedWhenDelegateFails() {
		CaslUserStatusProperties properties = new CaslUserStatusProperties();
		properties.setCacheTtl(Duration.ofSeconds(60));
		properties.setStaleGrace(Duration.ofSeconds(300));
		StubDelegate delegate = new StubDelegate();
		MutableClock clock = new MutableClock();
		CachingUserStatusVerifier verifier = new CachingUserStatusVerifier(delegate, properties, clock);

		UserStatus first = verifier.verify(jwt("sub-1"));
		assertThat(first).isEqualTo(UserStatus.ACTIVE);
		assertThat(delegate.invocations.get()).isEqualTo(1);

		clock.advance(Duration.ofSeconds(61));
		delegate.failure = new RuntimeException("users down");
		UserStatus second = verifier.verify(jwt("sub-1"));

		assertThat(second).isEqualTo(UserStatus.ACTIVE);
		assertThat(delegate.invocations.get()).isEqualTo(2);
	}

	@Test
	void jwtWithoutSubjectFailsClosed() {
		CaslUserStatusProperties properties = new CaslUserStatusProperties();
		CachingUserStatusVerifier verifier = new CachingUserStatusVerifier(new StubDelegate(), properties,
				new MutableClock());

		Jwt jwtWithoutSubject = mock(Jwt.class);
		assertThatThrownBy(() -> verifier.verify(jwtWithoutSubject))
				.isInstanceOf(UserStatusUnavailableException.class);
	}

	@Test
	void failureWithoutFreshOrStaleEntryThrowsUnavailable() {
		CaslUserStatusProperties properties = new CaslUserStatusProperties();
		properties.setCacheTtl(Duration.ZERO);
		properties.setStaleGrace(Duration.ZERO);
		CachingUserStatusVerifier verifier = new CachingUserStatusVerifier(new FailingDelegate(), properties,
				new MutableClock());

		assertThatThrownBy(() -> verifier.verify(jwt("sub-1")))
				.isInstanceOf(UserStatusUnavailableException.class);
	}

	private static final class StubDelegate implements UserStatusVerifier {

		private UserStatus status = UserStatus.ACTIVE;

		private RuntimeException failure;

		private final AtomicInteger invocations = new AtomicInteger();

		@Override
		public UserStatus verify(Jwt jwt) {
			invocations.incrementAndGet();
			if (failure != null) {
				throw failure;
			}
			return status;
		}
	}

	private static final class FailingDelegate implements UserStatusVerifier {

		@Override
		public UserStatus verify(Jwt jwt) {
			throw new RuntimeException("users down");
		}
	}

	private static final class MutableClock extends Clock {

		private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
