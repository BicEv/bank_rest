package com.example.bankcards.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.bankcards.TestSecurityConfig;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.exception.DuplicateUsernameException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserRestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(TestSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private UserDto sampleUser() {
        return new UserDto(1L, "john_doe", "John Doe", Role.USER, List.of());
    }

    private UserRequest sampleRequest() {
        return new UserRequest("john_doe", "John Doe", "password", Role.USER);
    }

    // Create user
    @Test
    void createUser_success() throws Exception {
        var created = sampleUser();
        var request = sampleRequest();

        when(userService.createUser(request)).thenReturn(created);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/users/1"))
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void createUser_validationError() throws Exception {
        UserRequest invalidRequest = new UserRequest("", "John Doe", "password", Role.USER);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].message").value("username: Username cannot be empty"))
                .andExpect(jsonPath("$[0].statusCode").value(400));
    }

    @Test
    void createUser_duplicateUsername() throws Exception {
        var request = sampleRequest();

        Mockito.doThrow(new DuplicateUsernameException("Username already in use"))
                .when(userService).createUser(request);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already in use"))
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    // Get user by id
    @Test
    void getUserById_success() throws Exception {
        when(userService.getUserById(1L)).thenReturn(sampleUser());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("john_doe"));
    }

    @Test
    void getUserById_notFound() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // Get all users
    @Test
    void getAllUsers_success() throws Exception {
        Page<UserDto> page = new PageImpl<>(List.of(sampleUser()), PageRequest.of(0, 10), 1);
        when(userService.getAllUsers(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("john_doe"));
    }

    // Update user
    @Test
    void updateUser_success() throws Exception {
        UserRequest request = new UserRequest("john", "John Updated", "newpass", Role.USER);
        UserDto updated = new UserDto(1L, "john", "John Updated", Role.USER, List.of());
        when(userService.updateUser(1L, request)).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Updated"));
    }

    @Test
    void updateUser_notFound() throws Exception {
        UserRequest request = new UserRequest("john", "John Updated", "newpass", Role.USER);

        when(userService.updateUser(999L, request))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    // Delete user
    @Test
    void deleteUser_success() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_notFound() throws Exception {
        Mockito.doThrow(new UserNotFoundException("User not found"))
                .when(userService).deleteUser(999L);

        mockMvc.perform(delete("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

}
