package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;

public record UserRequest(String username, String fullName, String password, Role role) {

}
