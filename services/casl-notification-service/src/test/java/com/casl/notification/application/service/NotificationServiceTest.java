package com.casl.notification.application.service;

import com.casl.notification.domain.model.DomainEventEnvelope;
import com.casl.notification.domain.port.InboxRepository;
import com.casl.notification.domain.port.NotificationChannel;
import com.casl.notification.domain.port.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

	private static final UUID EVENT_ID = UUID.fromString("3f1c2a7e-0000-0000-0000-000000000009");

	@Test
	void processesEventOnceAndSkipsDuplicate() {
		InboxRepository inbox = mock(InboxRepository.class);
		NotificationRepository notificationRepository = mock(NotificationRepository.class);
		NotificationChannel channel = mock(NotificationChannel.class);
		when(channel.channelName()).thenReturn("log");
		when(inbox.alreadyProcessed(EVENT_ID)).thenReturn(false, true);

		NotificationService service = new NotificationService(notificationRepository, inbox, List.of(channel),
				java.time.Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), java.time.ZoneOffset.UTC));

		DomainEventEnvelope event = new DomainEventEnvelope(EVENT_ID, "order.created.v1", "order-1",
				Instant.parse("2026-01-01T10:00:00Z"), 1, "trace-1", java.util.Map.of());

		service.process(event);
		service.process(event);

		verify(channel, times(1)).send(org.mockito.ArgumentMatchers.any());
		verify(inbox, times(1)).markProcessed(EVENT_ID, "order.created.v1");
	}

	@Test
	void skipsAlreadyProcessedEvent() {
		InboxRepository inbox = mock(InboxRepository.class);
		NotificationRepository notificationRepository = mock(NotificationRepository.class);
		NotificationChannel channel = mock(NotificationChannel.class);
		when(channel.channelName()).thenReturn("log");
		when(inbox.alreadyProcessed(EVENT_ID)).thenReturn(true);

		NotificationService service = new NotificationService(notificationRepository, inbox, List.of(channel),
				java.time.Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), java.time.ZoneOffset.UTC));

		DomainEventEnvelope event = new DomainEventEnvelope(EVENT_ID, "order.created.v1", "order-1",
				Instant.parse("2026-01-01T10:00:00Z"), 1, "trace-1", java.util.Map.of());

		service.process(event);

		verify(channel, org.mockito.Mockito.never()).send(org.mockito.ArgumentMatchers.any());
	}
}
