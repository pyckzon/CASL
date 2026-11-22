package com.casl.notification.adapter.out.channel;

import com.casl.notification.domain.model.Notification;
import com.casl.notification.domain.port.NotificationChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class EmailNotificationChannel implements NotificationChannel {

	@Override
	public String channelName() {
		return "email";
	}

	@Override
	public void send(Notification notification) {
		// Simulated email: in production, delegate to SMTP adapter.
		log.info("[EMAIL] to={} subject={} body={}", notification.recipient(), notification.subject(), notification.body());
	}
}
