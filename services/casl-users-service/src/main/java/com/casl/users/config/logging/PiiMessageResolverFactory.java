package com.casl.users.config.logging;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolver;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverContext;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverFactory;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverConfig;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolverFactory;

/**
 * Factory del event resolver JSON {@code piiMessage} usado por el event template
 * {@code casl-log-event-pii.json} del {@code JsonTemplateLayout}.
 */
@Plugin(name = "PiiMessageResolverFactory", category = TemplateResolverFactory.CATEGORY)
public final class PiiMessageResolverFactory implements EventResolverFactory {

	private static final PiiMessageResolverFactory INSTANCE = new PiiMessageResolverFactory();

	private PiiMessageResolverFactory() {
	}

	@PluginFactory
	public static PiiMessageResolverFactory getInstance() {
		return INSTANCE;
	}

	@Override
	public String getName() {
		return "piiMessage";
	}

	@Override
	public EventResolver create(EventResolverContext context, TemplateResolverConfig config) {
		return new PiiMessageResolver();
	}
}
