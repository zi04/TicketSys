\set reservation_id '40000000-0000-0000-0000-000000000001'
\set event_id '11111111-1111-1111-1111-111111111111'
\set ticket_id 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
\set customer_id 'customer-7'
\set quantity 2
\set current_time '2026-08-30 12:00:00+00'
BEGIN;

--R9 from week 1
SELECT id FROM ticket_types
    WHERE id = :'ticket_id'::uuid AND event_id = :'event_id'::uuid
    FOR UPDATE;

SELECT
    allocation - reserved - sold >= :quantity
    AS available_seats
    FROM ticket_types WHERE id = :'ticket_id'::uuid
    \gset

\if :available_seats
INSERT INTO reservations(
    id,
    event_id,
    customer_id,
    status,
    created_at,
    expires_at
) VALUES (
     :'reservation_id'::uuid,
     :'event_id'::uuid,
     :'customer_id',
     'PENDING',
     :'current_time'::timestamp,
      :'current_time'::timestamp + INTERVAL '15 minutes'
);

INSERT INTO  reservation_lines (
    reservation_id,
    event_id,
    ticket_id,
    ticket_name,
    price,
    currency,
    quantity
)
SELECT
    :'reservation_id'::uuid,
    :'event_id'::uuid,
    id,
    name,
    price,
    'USD',
    :quantity
    FROM ticket_types WHERE id = :'ticket_id'::uuid;


UPDATE ticket_types SET reserved = reserved + :quantity
WHERE id = :'ticket_id'::uuid;

COMMIT;

\echo 'Reservation created successfully.'
\else
ROLLBACK;
\echo 'Reservation rejected: not enough seats.'
\endif

