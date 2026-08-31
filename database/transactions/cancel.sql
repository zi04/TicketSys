-- R11 from week 1
-- R12 from week 1

\set reservation_id '40000000-0000-0000-0000-000000000002'
\set customer_id 'customer-8'
\set current_time '2026-08-30 12:05:00+00'

BEGIN;

SELECT id
    FROM reservations
    WHERE id = :'reservation_id'::uuid
FOR UPDATE;

SELECT EXISTS (
    SELECT 1
    FROM reservations
    WHERE id = :'reservation_id'::uuid
    AND customer_id = :'customer_id'
    AND status = 'PENDING'
    AND expires_at > :'current_time'::timestamp
) AS can_cancel
\gset

\if :can_cancel

UPDATE ticket_types tt
    SET reserved = tt.reserved - rl.quantity
    FROM reservation_lines rl
    WHERE rl.reservation_id = :'reservation_id'::uuid
    AND tt.id = rl.ticket_id;

UPDATE reservations
    SET status = 'CANCELLED'
    WHERE id = :'reservation_id'::uuid;

COMMIT;
\echo 'Reservation cancelled successfully.'


\else
ROLLBACK;
\echo 'Cancellation rejected. reservation is not pending, has expired, or belongs to another customer.'
\endif

