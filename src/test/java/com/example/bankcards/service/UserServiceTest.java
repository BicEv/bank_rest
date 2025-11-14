package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateUsernameException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void init() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    private User createUser(Long id) {
        return User.builder()
                .id(id)
                .username("john_doe")
                .fullName("John Doe")
                .role(Role.USER)
                .password("encoded")
                .cards(Collections.emptyList())
                .build();
    }

    private UserRequest getUserRequest() {
        return new UserRequest("john_doe", "John Doe", "password", Role.USER);
    }

    // Create
    @Test
    void createUser_success() {
        UserRequest userRequest = getUserRequest();

        when(userRepository.existsByUsername(userRequest.username())).thenReturn(false);
        when(passwordEncoder.encode(userRequest.password())).thenReturn("encoded");

        User saved = createUser(1L);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserDto result = userService.createUser(userRequest);

        assertEquals(1L, result.id());
        assertEquals(saved.getUsername(), result.username());
        assertEquals(saved.getRole(), result.role());
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateUsername() {
        UserRequest userRequest = getUserRequest();

        when(userRepository.existsByUsername(userRequest.username())).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> userService.createUser(userRequest));
    }

    // Get by id

    @Test
    void getUserById_success() {
        User saved = createUser(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(saved));

        UserDto result = userService.getUserById(10L);

        assertEquals(10L, result.id());
        assertEquals(saved.getFullName(), result.fullName());
        assertEquals(saved.getRole(), result.role());

        verify(userRepository).findById(10L);
    }

    @Test
    void getUserById_userNotFound() {
        when(userRepository.findById(15L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(15L));
    }

    // Get by fullname
    @Test
    void getUserByFullname_success() {
        User saved = createUser(20L);
        when(userRepository.findByFullName("John Doe")).thenReturn(Optional.of(saved));

        UserDto result = userService.getUserByFullname("John Doe");

        assertEquals(20L, result.id());
        assertEquals("John Doe", result.fullName());
        assertEquals(saved.getRole(), result.role());
    }

    @Test
    void getUserByFullname_userNotFound() {
        when(userRepository.findByFullName("John Doe")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserByFullname("John Doe"));
    }

    // Get all users
    @Test
    void getAllUsers_success() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> users = List.of(createUser(25L), createUser(30L));
        Page<User> page = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<UserDto> result = userService.getAllUsers(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals("john_doe", result.getContent().get(0).username());
    }

    // Update user
    @Test
    void updateUser_success() {
        User existing = createUser(35L);
        when(userRepository.findById(35L)).thenReturn(Optional.of(existing));

        UserRequest request = new UserRequest("new_john", "John Alan Doe", "newpass", Role.USER);

        when(userRepository.existsByUsername("new_john")).thenReturn(false);
        when(passwordEncoder.encode("newpass")).thenReturn("encoded-newpass");

        User updated = User.builder()
                .id(35L)
                .username("new_john")
                .fullName("John Alan Doe")
                .password("encoded-newpass")
                .role(Role.USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(updated);

        UserDto result = userService.updateUser(35L, request);

        assertEquals(updated.getUsername(), result.username());
        assertEquals(updated.getFullName(), result.fullName());
        assertEquals(updated.getRole(), result.role());
    }

    @Test
    void updateUser_duplicateUser() {
        User existing = createUser(40L);
        when(userRepository.findById(40L)).thenReturn(Optional.of(existing));

        UserRequest request = new UserRequest("existing", "NaN", "null", Role.USER);
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThrows(DuplicateUsernameException.class, () -> userService.updateUser(40L, request));
    }

    @Test
    void duplicateUser_userNotFound() {
        when(userRepository.findById(45L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(45L, getUserRequest()));
    }

    // Delete user
    @Test
    void deleteUser_success() {
        User existing = createUser(50L);
        when(userRepository.findById(50L)).thenReturn(Optional.of(existing));

        userService.deleteUser(50L);

        verify(userRepository).deleteById(50L);
    }

    @Test
    void deleteUser_userNotFound() {
        when(userRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(55L));
    }

}
