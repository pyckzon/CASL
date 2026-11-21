package com.casl.users.application.query;

import com.casl.users.domain.model.UserStatus;

/**
 * Consulta paginada. Los filtros son opcionales y el email se normaliza en el
 * caso de uso.
 */
public record UserQuery(UserStatus status, String email, int page, int size) {
}
