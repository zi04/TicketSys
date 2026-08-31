\set customer_id 'customer-2'

Select
    r.id AS reservation_id,
    e.name AS event,
    r.status,
    SUM(rl.price * rl.quantity) AS total_amount,
    'USD' AS currency,
    r.created_at FROM reservations r
    JOIN events e ON e.id = r.event_id
    JOIN reservation_lines rl ON rl.reservation_id = r.id
    WHERE r.customer_id = :'customer_id'
    GROUP BY
    r.id,
    e.name,
    r.status,
    r.created_at
    ORDER BY r.created_at DESC;
