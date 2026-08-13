package com.sohan.codedocs.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users", indexes = @Index(name = "idx_users_email", columnList = "email"))
public class User extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String email;

    /** BCrypt hash — never the raw password, never logged, never serialized. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
