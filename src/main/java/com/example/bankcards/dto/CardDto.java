package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.bankcards.entity.CardStatus;

public record CardDto(
        UUID id,
        String maskedNumber,
        String ownerFullName,
        Integer expiryYear,
        Integer expiryMonth,
        CardStatus cardStatus,
        BigDecimal balance) {

}
