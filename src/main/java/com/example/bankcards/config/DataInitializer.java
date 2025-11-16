package com.example.bankcards.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;

@Configuration
public class DataInitializer {

    private final UserRepository userRepository;

    DataInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setUsername("ADMIN");
                admin.setFullName("Admin_account");
                admin.setPassword(passwordEncoder.encode("AdMiN"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);

            }
        };
    }

}
