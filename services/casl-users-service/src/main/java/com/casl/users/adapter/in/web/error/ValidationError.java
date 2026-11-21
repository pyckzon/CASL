package com.casl.users.adapter.in.web.error;

/**
 * Detalle de un campo inválido dentro de un error de validación.
 */
public record ValidationError(String field, String message) {
}
