package com.casl.gateway.config;

import com.casl.security.CaslSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * El borde usa la configuración de seguridad compartida (deny-by-default,
 * JWT de Keycloak y errores RFC 9457). El cableado se importa explícitamente
 * para que cada aplicación declare sus dependencias de seguridad.
 */
@Configuration(proxyBeanMethods = false)
@Import(CaslSecurityConfiguration.class)
public class SecurityConfig {
}
