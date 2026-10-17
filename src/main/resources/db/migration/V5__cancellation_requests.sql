CREATE TABLE cancellation_requests (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL,
    order_id uuid NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    reason varchar(40) NOT NULL,
    note varchar(300),
    status varchar(20) NOT NULL,
    requested_at timestamptz NOT NULL,
    decided_by uuid REFERENCES users (id),
    decided_at timestamptz,
    decision_note varchar(300)
);

CREATE INDEX cancellation_requests_pending_requested_at_idx ON cancellation_requests (requested_at)
    WHERE status = 'PENDING';
