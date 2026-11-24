package com.casl.security.userstatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Decorador de {@link UserStatusVerifier} con caché por subject. TTL corto
 * (60 s) y margen de obsolescencia (300 s): si {@code users} no responde, se
 * reutiliza el último estado conocido dentro del margen; fuera de él se falla
 * cerrado con {@link UserStatusUnavailableException}.
 */
public class CachingUserStatusVerifier implements UserStatusVerifier {

	private static final Logger log = LoggerFactory.getLogger(CachingUserStatusVerifier.class);

	private final UserStatusVerifier delegate;
	private final Duration cacheTtl;
	private final Duration staleGrace;
	private final Clock clock;
	private final Map<String, Entry> cache = new ConcurrentHashMap<>();

	public CachingUserStatusVerifier(UserStatusVerifier delegate, CaslUserStatusProperties properties) {
		this(delegate, properties, Clock.systemUTC());
	}

	public CachingUserStatusVerifier(UserStatusVerifier delegate, CaslUserStatusProperties properties, Clock clock) {
		this.delegate = delegate;
		this.cacheTtl = properties.getCacheTtl();
		this.staleGrace = properties.getStaleGrace();
		this.clock = clock;
	}

	@Override
	public UserStatus verify(Jwt jwt) {
		String subject = jwt.getSubject();
		if (subject == null || subject.isBlank()) {
			throw new UserStatusUnavailableException("JWT without subject cannot be verified");
		}
		Instant now = clock.instant();
		Entry entry = cache.get(subject);
		if (isFresh(entry, now)) {
			return entry.status();
		}
		try {
			UserStatus status = delegate.verify(jwt);
			cache.put(subject, new Entry(status, now));
			return status;
		}
		catch (RuntimeException exception) {
			Entry stale = cache.get(subject);
			if (isWithinGrace(stale, now)) {
				log.warn("User status cache stale for subject={} status={} ageSeconds={}",
						subject, stale.status(), Duration.between(stale.cachedAt(), now).toSeconds());
				return stale.status();
			}
			throw new UserStatusUnavailableException("Unable to verify user status for subject " + subject, exception);
		}
	}

	private boolean isFresh(Entry entry, Instant now) {
		return entry != null && Duration.between(entry.cachedAt(), now).compareTo(cacheTtl) <= 0;
	}

	private boolean isWithinGrace(Entry entry, Instant now) {
		return entry != null && Duration.between(entry.cachedAt(), now).compareTo(cacheTtl.plus(staleGrace)) <= 0;
	}

	private record Entry(UserStatus status, Instant cachedAt) {
	}
}
