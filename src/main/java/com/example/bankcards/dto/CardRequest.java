package com.example.bankcards.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Запрос на создание или обновление карты")
public record CardRequest(

                @Schema(description = "Номер карты (16 цифр)", example = "1234567812345678") 
                @NotBlank @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits long") 
                String plainNumber,

                @Schema(description = "Год окончания действия карты", example = "2030")
                @Min(value = 2025) @Max(value = 2100) 
                Integer expiryYear,

                @Schema(description = "Месяц окончания действия карты", example = "10")
                @Min(value = 1) @Max(value = 12) 
                Integer expiryMonth,

                @Schema(description = "Начальный баланс карты", example = "100.00")
                @DecimalMin(value = "0.00") 
                BigDecimal initialBalance) {

}
