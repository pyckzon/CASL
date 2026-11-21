package com.casl.users.domain.model;

/**
 * Filtro de búsqueda de usuarios. Todos los campos son opcionales.
 */
public record UserFilter(UserStatus status, Email email) {
}
