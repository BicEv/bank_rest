package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
        @NotBlank(message = "Username cannot be empty") String username,
        @NotBlank(message = "Full name cannot be empty") String fullName,
        @NotBlank(message = "Password cannot be empty") String password,
        @NotNull(message = "Role is required") Role role) {

}
