package com.example.bankcards.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CardRequest(

        @NotBlank @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits long") String plainNumber,
        @Min(value = 2025) @Max(value = 2100) Integer expiryYear,
        @Min(value = 1) @Max(value = 12) Integer expiryMonth,
        @DecimalMin(value = "0.00") BigDecimal initialBalance) {

}
