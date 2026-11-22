package com.casl.notification.domain.port;

import java.util.UUID;

/**
 * Inbox para idempotencia de consumidores: registra el eventId procesado.
 */
public interface InboxRepository {
	boolean alreadyProcessed(UUID eventId);
	void markProcessed(UUID eventId, String eventType);
}
