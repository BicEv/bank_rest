package com.example.bankcards.dto;

import java.math.BigDecimal;

public record CardRequest(
        String plainNumber,
        Integer expiryYear,
        Integer expiryMonth,
        BigDecimal initialBalance) {

}
