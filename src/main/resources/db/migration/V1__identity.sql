CREATE TABLE users (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    disabled_at timestamptz,
    name varchar(120) NOT NULL,
    email varchar(254) NOT NULL,
    password_hash varchar(100) NOT NULL,
    role varchar(20) NOT NULL
);

CREATE UNIQUE INDEX users_email_lower_key ON users (lower(email));

CREATE TABLE refresh_tokens (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    family_id uuid NOT NULL,
    expires_at timestamptz NOT NULL,
    family_expires_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE INDEX refresh_tokens_family_id_idx ON refresh_tokens (family_id);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);

CREATE INDEX refresh_tokens_family_expires_at_idx ON refresh_tokens (family_expires_at);
