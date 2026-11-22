package com.casl.catalog.config;

import com.casl.security.CaslSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Resource server del catálogo. La configuración concreta vive en
 * {@code casl-security}; aquí solo se declara la importación explícita.
 */
@Configuration(proxyBeanMethods = false)
@Import(CaslSecurityConfiguration.class)
public class SecurityConfig {
}
