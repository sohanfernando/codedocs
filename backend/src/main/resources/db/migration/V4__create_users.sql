-- ---------------------------------------------------------------------------
-- Accounts. Email + BCrypt hash — no external identity provider.
-- ---------------------------------------------------------------------------
CREATE TABLE users (
                        id            UUID         PRIMARY KEY,
                        email         VARCHAR(255) NOT NULL,
                        password_hash VARCHAR(100) NOT NULL,
                        created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Case-insensitive uniqueness: "Sohan@x.com" and "sohan@x.com" are the same
-- account, or login/registration would behave inconsistently between them.
CREATE UNIQUE INDEX uq_users_email ON users (LOWER(email));
