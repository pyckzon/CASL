package com.casl.shipping.domain.port;

import java.util.UUID;

public interface InboxRepository {
	boolean alreadyProcessed(UUID eventId);
	void markProcessed(UUID eventId, String eventType);
}
