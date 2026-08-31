SELECT
    e.id AS event_id,
    e.name AS event,

    SUM(tt.allocation) AS total_allocation,
    SUM(tt.sold) AS total_sold,

    ( SUM(tt.sold)::NUMERIC
    / SUM(tt.allocation) ) * 100 AS sold_percentage
    FROM events e
    JOIN ticket_types tt ON tt.event_id = e.id
    GROUP BY e.id, e.name

    HAVING SUM(tt.sold)::NUMERIC / SUM(tt.allocation) > 0.80
    ORDER BY sold_percentage DESC;