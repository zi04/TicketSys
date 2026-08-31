SET TIME ZONE 'UTC';

CREATE TYPE event_status AS ENUM (
    'DRAFT', 'PUBLISHED'
);

CREATE TABLE events (
    id UUID PRIMARY KEY,

    organiser_id TEXT NOT NULL,
    name TEXT NOT NULL,

    sales_open_at TIMESTAMPTZ NOT NULL,
    sales_close_at TIMESTAMPTZ NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,

    status event_status NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT check_event_sales
    CHECK (
    sales_open_at <= sales_close_at
    AND sales_close_at <= starts_at
    ),

    CONSTRAINT check_event_name
    CHECK (trim(name) <> ''),

    CONSTRAINT chk_event_organiser
    CHECK (trim(organiser_id) <> '')
);

CREATE TABLE ticket_types(
    id UUID NOT NULL,
    event_id UUID NOT NULL,
    name TEXT NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    currency CHAR (3) NOT NULL,
    allocation INTEGER NOT NULL,
    per_customer_limit INTEGER NOT NULL,
    reserved INTEGER NOT NULL DEFAULT 0,
    sold INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_ticket_type_event
    FOREIGN KEY (event_id) REFERENCES events (id),

    CONSTRAINT un_ticket_type_eid
    UNIQUE(event_id, id),

    CONSTRAINT check_ticket_price
    CHECK (price>=0),

    CONSTRAINT check_ticket_name
    CHECK (trim(name)<>''),

    CONSTRAINT check_ticket_currency
    CHECK ( currency = upper(currency) AND length(currency)=3),

    CONSTRAINT check_ticket_allocation
    CHECK (allocation > 0),

    CONSTRAINT check_ticket_customer_limit
    CHECK (per_customer_limit > 0
    AND per_customer_limit <= allocation),

    CONSTRAINT check_ticket_reserved
    CHECK (reserved >= 0),

    CONSTRAINT check_ticket_sold
    CHECK (sold >= 0),

    CONSTRAINT check_ticket_threshold
    CHECK (reserved + sold <= allocation)

);

CREATE VIEW ticket_availability AS SELECT
    id, event_id, name, price, currency, allocation, reserved,
    sold, allocation - reserved - sold AS available,
    per_customer_limit FROM ticket_types;


CREATE TYPE reservation_status AS ENUM (
    'PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'
);

CREATE TABLE reservations(
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    customer_id TEXT NOT NULL,
    status reservation_status NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_reservation_event
    FOREIGN KEY (event_id) REFERENCES events(id),

    CONSTRAINT un_reservation_eid
    UNIQUE (event_id, id),

    CONSTRAINT check_reservation_customer
    CHECK (trim(customer_id)<>''),

    CONSTRAINT check_reservation_expiry
    CHECK (expires_at >created_at)
);


CREATE UNIQUE INDEX pending_customer
    ON reservations(customer_id, event_id)
    WHERE status = 'PENDING';


CREATE TABLE reservation_lines(
    reservation_id UUID NOT NULL,
    event_id UUID NOT NULL,
    ticket_id UUID NOT NULL,
    ticket_name TEXT NOT NULL,
    price NUMERIC (12,2) NOT NULL,
    currency CHAR(3) NOT NULL,
    quantity INTEGER NOT NULL,

    PRIMARY KEY (reservation_id,ticket_id),

    CONSTRAINT fk_reservation_line
    FOREIGN KEY (event_id, reservation_id)
    REFERENCES reservations(event_id,id),

    CONSTRAINT fk_reservation_line_ticket
    FOREIGN KEY (event_id, ticket_id)
    REFERENCES ticket_types(event_id,id),


    CONSTRAINT check_reservation_line_name
    CHECK (trim(ticket_name)<>''),

    CONSTRAINT check_reservation_line_price
    CHECK (price >= 0),

    CONSTRAINT check_reservation_line_currency
    CHECK ( currency = upper(currency)
    AND length(currency) = 3),

    CONSTRAINT check_reservation_line_quantity
    CHECK (quantity > 0 )

);


CREATE TYPE payment_status AS ENUM(
'APPROVED', 'DECLINED' );

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL,
    payment_reference TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    status payment_status NOT NULL,
    attempted_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_payment_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservations(id),

    CONSTRAINT chk_payment_reference_not_blank
    CHECK (trim(payment_reference) <> ''),

    CONSTRAINT chk_payment_amount
    CHECK (amount >= 0),

    CONSTRAINT chk_payment_currency
    CHECK (
    currency = upper(currency)
    AND length(currency) = 3
    )
);

-- D5
CREATE UNIQUE INDEX un_approved_payment
    ON payments (payment_reference)
    WHERE status = 'APPROVED';

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL UNIQUE,
    payment_id UUID NOT NULL UNIQUE,
    event_id UUID NOT NULL,
    customer_id TEXT NOT NULL,
    total_amount NUMERIC (12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_order_reservation
    FOREIGN KEY (reservation_id)
    REFERENCES reservations(id),

    CONSTRAINT fk_order_event
    FOREIGN KEY (event_id)
    REFERENCES events(id),

    CONSTRAINT check_order_empty
    CHECK (trim(customer_id)<>''),

    CONSTRAINT check_order_total
    CHECK (total_amount>= 0),

    CONSTRAINT check_order_currency
    CHECK (
    currency= upper (currency)
    AND length(currency)=3
    )


);