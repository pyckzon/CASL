package com.casl.notification.adapter.out.channel;

import com.casl.notification.domain.model.Notification;
import com.casl.notification.domain.port.NotificationChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class WebhookNotificationChannel implements NotificationChannel {

	@Override
	public String channelName() {
		return "webhook";
	}

	@Override
	public void send(Notification notification) {
		// Simulated webhook: in production, POST to a configured URL.
		log.info("[WEBHOOK] to={} subject={} body={}", notification.recipient(), notification.subject(), notification.body());
	}
}
