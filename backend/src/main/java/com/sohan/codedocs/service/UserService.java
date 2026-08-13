package com.sohan.codedocs.service;

import com.sohan.codedocs.entity.User;

import java.util.UUID;

public interface UserService {
    User register(String email, String password);

    User findByIdOrThrow(UUID id);
}
