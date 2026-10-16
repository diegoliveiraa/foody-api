CREATE TABLE IF NOT EXISTS sample_entity (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    name varchar(100) NOT NULL
);
