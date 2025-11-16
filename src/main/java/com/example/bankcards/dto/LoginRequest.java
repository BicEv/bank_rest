package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
@Schema(description = "Данные пользователя для авторизации")
public record LoginRequest(

        @Schema(description = "Имя пользователя",example = "john_doe")
        @NotEmpty(message = "Username cannot be empty") 
        String username,

        @Schema(description = "Пароль пользователя", example = "Pa$$W0rD")
        @NotEmpty(message = "Password cannot be empty") 
        String password) {

}
