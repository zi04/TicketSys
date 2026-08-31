BEGIN;

INSERT INTO events (
    id,
    organiser_id,
    name,
    sales_open_at,
    sales_close_at,
    starts_at,
    status
)
SELECT
    md5('gen-event-' || n)::uuid,
    'gen-organiser-' || n,
    'Gen Event ' ||n,
    '2026-09-01 18:00:00+00'::timestamptz
    + n * INTERVAL '1 hour' - INTERVAL '60 days',

    '2026-09-01 18:00:00+00'::timestamptz
    + n * INTERVAL '1 hour' - INTERVAL '1 day',

    '2026-09-01 18:00:00+00'::timestamp
    + n * INTERVAL '1 hour',

    'PUBLISHED'
    FROM generate_series(1,5000) AS n;


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
SELECT
    md5('gen-ticket-' || n)::uuid,
    md5('gen-event-' || n)::uuid,
    'Standard',
    20.00 + (n % 5) * 5,
    'USD',
    500,
    10,
    0,
    0
    FROM generate_series(1, 5000) AS n;

INSERT INTO reservations (
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
)
SELECT
    md5('gen-reservation-' || ((event_no - 1) * 50 + reservation_no))::uuid,
    md5('gen-event-' || event_no)::uuid,
    'gen-customer-' || (
        (((event_no - 1) * 50 + reservation_no) % 10000 ) + 1),

    CASE
        WHEN reservation_no % 4 = 0 THEN 'CONFIRMED'
        WHEN reservation_no % 4 = 1 THEN 'PENDING'
        WHEN reservation_no % 4 = 2 THEN 'CANCELLED'
        ELSE 'EXPIRED'
    END::reservation_status,

    ( '2026-09-01 18:00:00+00'::timestamptz
     + event_no * INTERVAL '1 hour'
     - INTERVAL '30 days' + reservation_no
     * INTERVAL '1 hour'),

    ('2026-09-01 18:00:00+00'::timestamptz
     + event_no * INTERVAL '1 hour'
     - INTERVAL '30 days'
     + reservation_no * INTERVAL '1 hour'
     + INTERVAL '15 minutes')

     FROM generate_series(1, 5000) AS event_no
     CROSS JOIN generate_series(1, 50) AS reservation_no;


INSERT INTO reservation_lines (
    reservation_id,
    event_id,
    ticket_id,
    ticket_name,
    price,
    currency,
    quantity
)
SELECT
    r.id,
    r.event_id,
    tt.id,
    tt.name,
    tt.price,
    'USD',
    1
    FROM reservations r
    JOIN ticket_types tt ON tt.event_id = r.event_id
    WHERE r.customer_id LIKE 'gen-customer-%';


UPDATE ticket_types tt
    SET reserved = counts.reserved_count,
    sold = counts.sold_count
    FROM (

    SELECT rl.ticket_id, COUNT(*) FILTER
    (WHERE r.status = 'PENDING')::INTEGER AS reserved_count,

    COUNT(*) FILTER
    (WHERE r.status = 'CONFIRMED')::INTEGER AS sold_count

    FROM reservations r
    JOIN reservation_lines rl ON rl.reservation_id = r.id
    WHERE r.customer_id LIKE 'gen-customer-%'
    GROUP BY rl.ticket_id)  AS counts
WHERE tt.id = counts.ticket_id;


INSERT INTO payments (
    id,
    reservation_id,
    payment_reference,
    amount,
    currency,
    status,
    attempted_at
)
SELECT
    md5('gen-payment-' || r.id::text)::uuid,
    r.id,
    'VOL-PAY-' || r.id,
    rl.price * rl.quantity,
    'USD',
    'APPROVED',
    r.created_at + INTERVAL '5 minutes'

    FROM reservations r
    JOIN reservation_lines rl ON rl.reservation_id = r.id
    WHERE r.customer_id LIKE 'gen-customer-%'
    AND r.status = 'CONFIRMED';


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
SELECT
    md5('gen-order-' || r.id::text)::uuid,
    r.id,
    r.event_id,
    r.customer_id,
    p.id,
    p.amount,
    'USD',
    p.attempted_at

    FROM reservations r
    JOIN payments p ON p.reservation_id = r.id
    AND p.status = 'APPROVED'
    WHERE r.customer_id LIKE 'gen-customer-%';

COMMIT;
