\set reservation_id '40000000-0000-0000-0000-000000000001'
\set customer_id 'customer-7'
\set payment_id '50000000-0000-0000-0000-000000000001'
\set order_id '60000000-0000-0000-0000-000000000001'
\set payment_reference 'PAY-3001'
\set amount 50.00
\set current_time '2026-08-30 12:05:00+00'
BEGIN;

-- R15 from week 1
SELECT EXISTS (
    SELECT 1
    FROM payments p
    JOIN orders o
    ON o.payment_id = p.id
    WHERE p.payment_reference = :'payment_reference'
    AND p.status = 'APPROVED'
) AS already_paid

\gset
\if :already_paid

SELECT
    o.id AS order_id,
    o.reservation_id,
    o.customer_id,
    o.total_amount,
    o.currency,
    o.created_at
    FROM orders o
    JOIN payments p
    ON p.id = o.payment_id
    WHERE p.payment_reference = :'payment_reference'
    AND p.status = 'APPROVED';

ROLLBACK;
\echo 'Payment reference was already used. returning existing order.'

\else
SELECT
    id FROM reservations WHERE id = :'reservation_id'::uuid
    FOR UPDATE;

-- R11 from week 1
SELECT EXISTS (
    SELECT 1 FROM reservations WHERE id = :'reservation_id'::uuid
    AND customer_id = :'customer_id'
    AND status = 'PENDING'
    AND expires_at > :'current_time'::timestamp
) AS reservation_ok

\gset

\if :reservation_ok

SELECT
    SUM(price * quantity) AS reservation_total
    FROM reservation_lines
    WHERE reservation_id = :'reservation_id'::uuid;

\gset

-- R13 from week 1
SELECT
    :amount::NUMERIC
    = :'reservation_total'::NUMERIC
    AS amount_ok

\gset

\if :amount_ok

-- R14 from week 1
UPDATE ticket_types tt
    SET reserved = tt.reserved - rl.quantity,
    sold = tt.sold + rl.quantity
    FROM reservation_lines rl
    WHERE rl.reservation_id = :'reservation_id'::uuid
    AND tt.id = rl.ticket_id;

UPDATE reservations
    SET status ='CONFIRMED'
    WHERE id = :'reservation_id'::uuid;

INSERT INTO payments (
    id,
    reservation_id,
    payment_reference,
    amount,
    currency,
    status,
    attempted_at
) VALUES (
    :'payment_id'::uuid,
    :'reservation_id'::uuid,
    :'payment_reference',
    :amount,
    'USD',
    'APPROVED',
    :'current_time'::timestamp

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
SELECT
    :'order_id'::uuid,
    r.id,
    r.event_id,
    r.customer_id,
    :'payment_id'::uuid,
    :reservation_total,
    'USD',
    :'current_time'::timestamp
FROM reservations r
WHERE r.id = :'reservation_id'::uuid;

COMMIT;
\echo 'Payment successful. Order created.'

\else
ROLLBACK;
\echo 'Payment rejected: incorrect payment amount.'
\endif

\else
ROLLBACK;
\echo 'Payment rejected: reservation is not pending, has expired, or belongs to another customer.'
\endif

\endif