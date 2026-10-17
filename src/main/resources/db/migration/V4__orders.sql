CREATE SEQUENCE order_number_seq START WITH 1001;

CREATE TABLE orders (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    number bigint NOT NULL UNIQUE,
    customer_id uuid NOT NULL REFERENCES users (id),
    status varchar(30) NOT NULL,
    placed_at timestamptz NOT NULL,
    delivery_zip_code varchar(9) NOT NULL,
    delivery_street varchar(120) NOT NULL,
    delivery_number varchar(10) NOT NULL,
    delivery_complement varchar(60),
    delivery_neighborhood varchar(60) NOT NULL,
    delivery_city varchar(60) NOT NULL,
    delivery_state char(2) NOT NULL,
    total numeric(12,2) NOT NULL,
    cancelled_by varchar(20),
    cancel_reason varchar(40),
    cancel_note varchar(300)
);

CREATE INDEX orders_status_placed_at_idx ON orders (status, placed_at);

CREATE INDEX orders_customer_id_placed_at_idx ON orders (customer_id, placed_at DESC);

CREATE INDEX orders_placed_at_idx ON orders (placed_at);

CREATE TABLE order_items (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products (id),
    product_name varchar(80) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    quantity integer NOT NULL CHECK (quantity BETWEEN 1 AND 20),
    subtotal numeric(12,2) NOT NULL
);

CREATE INDEX order_items_order_id_idx ON order_items (order_id);

CREATE INDEX order_items_product_id_idx ON order_items (product_id);

CREATE TABLE order_status_history (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status varchar(30),
    to_status varchar(30) NOT NULL,
    actor_id uuid NOT NULL REFERENCES users (id),
    actor_side varchar(20) NOT NULL,
    reason varchar(40),
    note varchar(300),
    occurred_at timestamptz NOT NULL
);

CREATE INDEX order_status_history_order_id_occurred_at_idx ON order_status_history (order_id, occurred_at);
