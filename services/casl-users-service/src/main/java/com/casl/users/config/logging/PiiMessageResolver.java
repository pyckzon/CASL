package com.casl.users.config.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * Event resolver JSON registrado como {@code piiMessage} en el event template
 * {@code casl-log-event-pii.json}: enmascara la PII del mensaje formateado antes
 * de escribir el campo {@code message} del log estructurado.
 *
 * <p>Reemplaza al resolver nativo {@code message} del {@code JsonTemplateLayout} para
 * delegar en {@link PiiMasker}. Se registra vía {@code PiiMessageResolverFactory}.
 */
public final class PiiMessageResolver implements EventResolver {

	public PiiMessageResolver() {
	}

	@Override
	public void resolve(LogEvent logEvent, JsonWriter jsonWriter) {
		org.apache.logging.log4j.message.Message message = logEvent == null ? null : logEvent.getMessage();
		String formatted = (message == null) ? "" : message.getFormattedMessage();
		if (formatted == null) {
			formatted = "";
		}
		jsonWriter.writeString(PiiMasker.mask(formatted));
	}
}
