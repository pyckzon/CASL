package com.casl.users.config.logging;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

/**
 * Converter de Log4j2 registrado como {@code %pii}: aplica {@link PiiMasker} al
 * mensaje ya formateado. Se declara en {@code log4j2-spring.xml} y el paquete
 * se escanea con el atributo {@code packages} de la configuración.
 */
@Plugin(name = "pii", category = PatternConverter.CATEGORY, elementType = PatternConverter.CATEGORY)
@ConverterKeys({ "pii" })
public final class PiiMaskingConverter extends LogEventPatternConverter {

	private PiiMaskingConverter() {
		super("pii", "pii");
	}

	@PluginFactory
	public static PiiMaskingConverter newInstance(String[] options) {
		return new PiiMaskingConverter();
	}

	@Override
	public void format(LogEvent event, StringBuilder toAppendTo) {
		toAppendTo.append(PiiMasker.mask(event.getMessage().getFormattedMessage()));
	}
}
