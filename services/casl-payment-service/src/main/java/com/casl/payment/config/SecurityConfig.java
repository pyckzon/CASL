package com.casl.payment.config;

import com.casl.security.CaslSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Resource server de pagos: JWT de usuario o de servicio (client credentials). */
@Configuration(proxyBeanMethods = false)
@Import(CaslSecurityConfiguration.class)
public class SecurityConfig {
}
