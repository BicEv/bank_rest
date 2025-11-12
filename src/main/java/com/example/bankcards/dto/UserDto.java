package com.example.bankcards.dto;

import java.util.List;

import com.example.bankcards.entity.Role;

public record UserDto(Long id, String username, String fullName, Role role, List<String> cards) {

}
