CREATE TABLE categories (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    disabled_at timestamptz,
    name varchar(60) NOT NULL,
    display_order integer NOT NULL
);

CREATE UNIQUE INDEX categories_name_lower_key ON categories (lower(name));

CREATE INDEX categories_display_order_idx ON categories (display_order);

CREATE TABLE products (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    disabled_at timestamptz,
    category_id uuid NOT NULL REFERENCES categories (id),
    name varchar(80) NOT NULL,
    description varchar(300),
    price numeric(12,2) NOT NULL CHECK (price > 0)
);

CREATE INDEX products_category_id_idx ON products (category_id);
