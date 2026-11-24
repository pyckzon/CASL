package com.casl.security.userstatus;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuración del cliente de estado de usuario en los desplegables que no
 * son dueños del dato ({@code casl.security.user-status.*}).
 */
@ConfigurationProperties("casl.security.user-status")
public class CaslUserStatusProperties {

	private String baseUrl = "http://localhost:8666";

	private Duration cacheTtl = Duration.ofSeconds(60);

	private Duration staleGrace = Duration.ofSeconds(300);

	private Duration connectTimeout = Duration.ofSeconds(1);

	private Duration readTimeout = Duration.ofSeconds(2);

	private boolean enabled = true;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public Duration getCacheTtl() {
		return cacheTtl;
	}

	public void setCacheTtl(Duration cacheTtl) {
		this.cacheTtl = cacheTtl;
	}

	public Duration getStaleGrace() {
		return staleGrace;
	}

	public void setStaleGrace(Duration staleGrace) {
		this.staleGrace = staleGrace;
	}

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
