package com.casl.cart.config;

import com.casl.security.userstatus.CaslUserStatusClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import(CaslUserStatusClientConfiguration.class)
public class UserStatusConfig {
}
