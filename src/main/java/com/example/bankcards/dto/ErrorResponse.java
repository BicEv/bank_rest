package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ошибка ответа API")
public record ErrorResponse(

    @Schema(description = "Описание ошибки", example = "Пользователь не найден")
    String message, 

    @Schema(description = "HTTP код ошибки", example = "404")
    int statusCode) {

}
