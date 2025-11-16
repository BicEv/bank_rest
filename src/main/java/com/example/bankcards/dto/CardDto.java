package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.bankcards.entity.CardStatus;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "Информация о банковской карте")
public record CardDto(

        @Schema(description = "Идентификатор карты", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID id,

        @Schema(description = "Маскированный номер карты", example = "**** **** **** 1234")
        String maskedNumber,

        @Schema(description = "ФИО владельца карты", example = "John Doe")
        String ownerFullName,

        @Schema(description = "Год окончания действия карты", example = "2030")
        Integer expiryYear,

        @Schema(description = "Месяц окончания действия карты", example = "10")
        Integer expiryMonth,

        @Schema(description = "Статус карты", example = "ACTIVE")
        CardStatus cardStatus,
        
        @Schema(description = "Баланс карты", example = "100.00")
        BigDecimal balance) {

}
