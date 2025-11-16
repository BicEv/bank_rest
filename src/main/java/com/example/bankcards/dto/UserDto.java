package com.example.bankcards.dto;

import java.util.List;

import com.example.bankcards.entity.Role;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO пользователя для ответов API")
public record UserDto(

        @Schema(description = "Идентификатор пользователя", example = "15") 
        Long id,

        @Schema(description = "Имя пользователя", example = "john_doe")
        String username,

        @Schema(description = "ФИО пользователя", example="John Alan Doe")
        String fullName,

        @Schema(description = "Роль пользователя", example = "USER")
        Role role,

        @Schema(description = "Список карт пользователя")
        List<String> cards) {

}
