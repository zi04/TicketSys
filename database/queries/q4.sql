-- per EVENT

SELECT
    e.id AS event_id,
    e.name AS event,
    SUM(o.total_amount) AS revenue,
    'USD' AS currency
    FROM events e JOIN orders o ON o.event_id = e.id
    GROUP BY e.id, e.name ORDER BY revenue DESC;


-- per TICKET TYPE

SELECT
    e.id AS event_id,
    e.name AS event,
    tt.id AS ticket_id,
    tt.name AS ticket_name,

    SUM(rl.price * rl.quantity) AS revenue,
    'USD' AS currency
    FROM orders o JOIN events e ON e.id = o.event_id
    JOIN reservation_lines rl ON rl.reservation_id = o.reservation_id
    JOIN ticket_types tt ON tt.id = rl.ticket_id

    GROUP BY e.id, e.name, tt.id, tt.name
    ORDER BY e.name, revenue DESC;
