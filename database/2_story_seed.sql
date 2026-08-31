BEGIN;

-- Published active
INSERT INTO events (
    id,
    organiser_id,
    name,
    sales_open_at,
    sales_close_at,
    starts_at,
    status)
    VALUES(
    '11111111-1111-1111-1111-111111111111',
    'Zaid',
    'test event 1',
    '2026-08-01 08:00:00+00',
    '2026-09-10 18:00:00+00',
    '2026-09-11 16:00:00+00',
    'PUBLISHED'
);

-- Draft
INSERT INTO events (
    id,
    organiser_id,
    name,
    sales_open_at,
    sales_close_at,
    starts_at,
    status
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'Aram',
    'Project discussion',
    '2026-09-01 08:00:00+00',
    '2026-10-10 18:00:00+00',
    '2026-10-11 16:00:00+00',
    'DRAFT'
);

--Published more than 80% sold.
INSERT INTO events (
    id,
    organiser_id,
    name,
    sales_open_at,
    sales_close_at,
    starts_at,
    status
)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'Aso',
    'Backend Workshop',
    '2026-08-01 08:00:00+00',
    '2026-09-19 18:00:00+00',
    '2026-09-20 16:00:00+00',
    'PUBLISHED'
);


INSERT INTO ticket_types (
    id,
    event_id,
    name,
    price,
    currency,
    allocation,
    per_customer_limit,
    reserved,
    sold
)
VALUES
(
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111',
    'General',
    25.00,
    'USD',
    100,
    5,
    3,
    2
),
(
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '11111111-1111-1111-1111-111111111111',
    'VIP',
    75.00,
    'USD',
    20,
    2,
    0,
    0
),
(
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    '22222222-2222-2222-2222-222222222222',
    'Early Bird',
    15.00,
    'USD',
    50,
    4,
    0,
    0
),
(
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '33333333-3333-3333-3333-333333333333',
    'Standard',
    20.00,
    'USD',
    10,
    10,
    0,
    9
);


-- Valid PENDING reservation.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'customer-1',
    'PENDING',
    '2026-08-30 11:50:00+00',
    '2026-08-30 12:05:00+00'
);


-- CONFIRMED reservation.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111111',
    'customer-2',
    'CONFIRMED',
    '2026-08-28 10:00:00+00',
    '2026-08-28 10:15:00+00'
);


-- CANCELLED reservation.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000003',
    '11111111-1111-1111-1111-111111111111',
    'customer-3',
    'CANCELLED',
    '2026-08-27 09:00:00+00',
    '2026-08-27 09:15:00+00'
);


-- Reservation already processed as EXPIRED.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000004',
    '11111111-1111-1111-1111-111111111111',
    'customer-4',
    'EXPIRED',
    '2026-08-26 08:00:00+00',
    '2026-08-26 08:15:00+00'
);


-- Expired hold the 15 minute rule from the previous assignment
-- still PENDING even though expires_at has already passed.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000005',
    '11111111-1111-1111-1111-111111111111',
    'customer-5',
    'PENDING',
    '2026-08-29 09:00:00+00',
    '2026-08-29 09:15:00+00'
);


-- Confirmed reservation for the nearly sold-out event.
INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
VALUES (
    '10000000-0000-0000-0000-000000000006',
    '33333333-3333-3333-3333-333333333333',
    'customer-6',
    'CONFIRMED',
    '2026-08-25 14:00:00+00',
    '2026-08-25 14:15:00+00'
);


INSERT INTO reservation_lines (
    reservation_id,
    event_id,
    ticket_id,
    ticket_name,
    price,
    currency,
    quantity
)
VALUES
(
    '10000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'General Admission',
    25.00,
    'USD',
    2
),
(
    '10000000-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'General Admission',
    25.00,
    'USD',
    2
),
(
    '10000000-0000-0000-0000-000000000003',
    '11111111-1111-1111-1111-111111111111',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'VIP',
    75.00,
    'USD',
    1
),
(
    '10000000-0000-0000-0000-000000000004',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'General Admission',
    25.00,
    'USD',
    1
),
(
    '10000000-0000-0000-0000-000000000005',
    '11111111-1111-1111-1111-111111111111',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'General Admission',
    25.00,
    'USD',
    1
),
(
    '10000000-0000-0000-0000-000000000006',
    '33333333-3333-3333-3333-333333333333',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'Standard',
    20.00,
    'USD',
    9
);


-- First attempt was declined.
INSERT INTO payments (
    id,
    reservation_id,
    payment_reference,
    amount,
    currency,
    status,
    attempted_at
)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'PAY-RETRY-001',
    50.00,
    'USD',
    'DECLINED',
    '2026-08-28 10:05:00+00'
);


-- Same reference retried and approved.
INSERT INTO payments (
    id,
    reservation_id,
    payment_reference,
    amount,
    currency,
    status,
    attempted_at
)
VALUES (
    '20000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000002',
    'PAY-RETRY-001',
    50.00,
    'USD',
    'APPROVED',
    '2026-08-28 10:07:00+00'
);


-- Another successful payment.
INSERT INTO payments (
    id,
    reservation_id,
    payment_reference,
    amount,
    currency,
    status,
    attempted_at
)
VALUES (
    '20000000-0000-0000-0000-000000000003',
    '10000000-0000-0000-0000-000000000006',
    'PAY-2001',
    180.00,
    'USD',
    'APPROVED',
    '2026-08-25 14:05:00+00'
);


INSERT INTO orders (
    id,
    reservation_id,
    event_id,
    customer_id,
    payment_id,
    total_amount,
    currency,
    created_at
)
VALUES (
    '30000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111111',
    'customer-2',
    '20000000-0000-0000-0000-000000000002',
    50.00,
    'USD',
    '2026-08-28 10:07:00+00'
);


INSERT INTO orders (
    id,
    reservation_id,
    event_id,
    customer_id,
    payment_id,
    total_amount,
    currency,
    created_at
)
VALUES (
    '30000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000006',
    '33333333-3333-3333-3333-333333333333',
    'customer-6',
    '20000000-0000-0000-0000-000000000003',
    180.00,
    'USD',
    '2026-08-25 14:05:00+00'
);

COMMIT;