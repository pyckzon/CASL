package com.casl.order.config;

import com.casl.security.CaslSecurityConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Resource server de pedidos: JWT validado en el propio servicio. */
@Configuration(proxyBeanMethods = false)
@Import(CaslSecurityConfiguration.class)
public class SecurityConfig {
}
