package com.casl.users.config;

import com.casl.security.CaslSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Resource server del servicio de usuarios. La configuración concreta vive en
 * {@code casl-security}; aquí solo se declara la importación explícita.
 */
@Configuration(proxyBeanMethods = false)
@Import(CaslSecurityConfiguration.class)
public class SecurityConfig {
}
