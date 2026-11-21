package com.casl.users.application.command;

/**
 * Datos de entrada para crear un usuario. Los valores llegan sin normalizar y
 * el caso de uso los convierte en value objects del dominio.
 */
public record CreateUserCommand(String email, String firstName, String lastName, String phone) {
}
