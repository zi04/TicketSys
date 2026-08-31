\set current_time '2026-08-30 12:10:00+00'

SELECT
    r.id AS reservation_id,
    r.customer_id,
    e.name AS event,
    r.created_at,
    r.expires_at
    FROM  reservations r
    JOIN events e ON e.id = r.event_id
    WHERE r.status = 'PENDING'
    -- cuurent time required by D10
    AND r.expires_at < :'current_time'::timestamptz
    ORDER BY r.expires_at ASC;