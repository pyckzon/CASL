package com.casl.notification.adapter.out.channel;

import com.casl.notification.domain.model.Notification;
import com.casl.notification.domain.port.NotificationChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class LogNotificationChannel implements NotificationChannel {

	@Override
	public String channelName() {
		return "log";
	}

	@Override
	public void send(Notification notification) {
		log.info("[NOTIFICATION:{}] to={} subject={} body={}", channelName(), notification.recipient(),
				notification.subject(), notification.body());
	}
}
