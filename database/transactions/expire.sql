-- R10 and R12 from week 1

\set reservation_id '10000000-0000-0000-0000-000000000005'
\set current_time '2026-08-30 12:00:00+00'
BEGIN;


SELECT id
    FROM reservations
    WHERE id = :'reservation_id'::uuid
    FOR UPDATE;

SELECT EXISTS (
    SELECT 1
    FROM reservations
    WHERE id = :'reservation_id'::uuid
    AND status = 'PENDING'
    AND expires_at <= :'current_time'::timestamp
) AS can_expire
\gset

\if :can_expire

UPDATE ticket_types tt
    SET reserved = tt.reserved - rl.quantity
    FROM reservation_lines rl
    WHERE rl.reservation_id = :'reservation_id'::uuid
    AND tt.id = rl.ticket_id;



UPDATE reservations
    SET status = 'EXPIRED'
    WHERE id = :'reservation_id'::uuid;

COMMIT;
\echo 'Reservation expired successfully.'
\else
ROLLBACK;
\echo 'Reservation cannot be expired.'
\endif
