package com.casl.security.userstatus;

import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Respuesta interna del estado del usuario: {@code {"status":"ACTIVE"|"INACTIVE"}}.
 * Sin PII: solo el estado.
 */
public record UserStatusResponse(UserStatus status) {
}
