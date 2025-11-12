package com.example.bankcards.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.bankcards.dto.UserDto;
import com.example.bankcards.dto.UserRequest;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.DuplicateUsernameException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto createUser(UserRequest userRequest) {
        if (userRepository.existsByUsername(userRequest.username())) {
            throw new DuplicateUsernameException("Username already in use: " + userRequest.username());
        }
        User user = User.builder()
                .username(userRequest.username())
                .fullName(userRequest.fullName())
                .password(passwordEncoder.encode(userRequest.password()))
                .role(userRequest.role())
                .build();
        logger.debug("User: {} was created, id: {}", user.getUsername(), user.getId());
        User savedUser = userRepository.save(user);
        return toDto(savedUser);

    }

    public UserDto getUserById(Long userId) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, id: " + userId));
        logger.debug("User found, id: {}", foundUser.getId());
        return toDto(foundUser);
    }

    public UserDto getUserByFullname(String fullname) {
        User foundUser = userRepository.findByFullName(fullname)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + fullname));
        logger.debug("User found, fullName: {}", foundUser.getFullName());
        return toDto(foundUser);
    }

    public Page<UserDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        logger.debug("All users requested");
        return users.map(this::toDto);
    }

    public UserDto updateUser(Long userId, UserRequest userRequest) {
        User foundUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found, id: " + userId));

        foundUser.setFullName(userRequest.fullName());

        if (userRequest.password() != null) {
            foundUser.setPassword(passwordEncoder.encode(userRequest.password()));
        }

        if (userRequest.username() != null && !userRequest.username().equals(foundUser.getUsername())) {
            if (userRepository.existsByUsername(userRequest.username())) {
                throw new DuplicateUsernameException("Username already in use: " + userRequest.username());
            }
            foundUser.setUsername(userRequest.username());
        }

        User updatedUser = userRepository.save(foundUser);
        logger.debug("User with id: {} was updated", userId);
        return toDto(updatedUser);
    }

    public void deleteUser(Long userId) {
        userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found, id: " + userId));
        userRepository.deleteById(userId);
        logger.debug("User with id: {} was deleted", userId);
    }

    private UserDto toDto(User user) {
        List<String> cards = user.getCards() != null
                ? List.copyOf(user.getCards().stream().map(card -> card.getMaskedNumber()).toList())
                : List.of();
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                cards);

    }

}
