package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на создание пользователя")
public record UserRequest(

        @Schema(description = "Имя пользователя",example = "john_doe")
        @NotBlank(message = "Username cannot be empty") 
        String username,

        @Schema(description = "ФИО пользователя", example = "John Alan Doe")
        @NotBlank(message = "Full name cannot be empty") 
        String fullName,

        @Schema(description = "Пароль пользователя", example = "Pa$$W0rD")
        @NotBlank(message = "Password cannot be empty") 
        String password,

        @Schema(description = "Роль пользователя", example = "ADMIN")
        @NotNull(message = "Role is required") 
        Role role) {

}
