package com.sohan.codedocs.service.impl;

import com.sohan.codedocs.entity.User;
import com.sohan.codedocs.exception.ApiException;
import com.sohan.codedocs.exception.NotFoundException;
import com.sohan.codedocs.repository.UserRepository;
import com.sohan.codedocs.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User register(String email, String password) {
        String normalised = email.toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalised)) {
            throw new ApiException("An account with that email already exists.");
        }

        User user = new User();
        user.setEmail(normalised);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
