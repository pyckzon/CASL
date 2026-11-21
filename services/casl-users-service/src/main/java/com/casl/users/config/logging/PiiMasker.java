package com.casl.users.config.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enmascara datos personales antes de que lleguen a un appender.
 *
 * <p>Se enmascaran los emails en cualquier posición y los teléfonos solo cuando
 * aparecen etiquetados ({@code phone=}, {@code telefono=}), porque un número
 * suelto es indistinguible de un id o de una marca de tiempo y enmascararlo
 * destruiría la utilidad del log.</p>
 */
public final class PiiMasker {

	private static final Pattern EMAIL = Pattern.compile(
			"([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

	private static final Pattern LABELED_PHONE = Pattern.compile(
			"(?i)\\b(phone|telefono|teléfono)\\s*[=:]\\s*([+0-9][0-9 ()-]{5,})");

	private static final String MASK = "***";

	private PiiMasker() {
	}

	public static String mask(String message) {
		if (message == null || message.isEmpty()) {
			return message;
		}
		String withoutEmails = EMAIL.matcher(message).replaceAll("$1" + MASK + "$2");
		return maskPhones(withoutEmails);
	}

	private static String maskPhones(String message) {
		Matcher matcher = LABELED_PHONE.matcher(message);
		StringBuilder masked = new StringBuilder();
		while (matcher.find()) {
			String digits = matcher.group(2).replaceAll("\\D", "");
			String visible = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
			String replacement = matcher.group(1) + "=" + MASK + visible;
			matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(masked);
		return masked.toString();
	}
}
