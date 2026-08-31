SELECT
    e.id,
    e.name,
    e.starts_at,
    SUM(t.allocation - t.reserved - t.sold) AS available
    FROM events e
    JOIN ticket_types t ON t.event_id = e.id
    WHERE e.status = 'PUBLISHED'
    GROUP BY e.id, e.name, e.starts_at
    HAVING SUM(t.allocation - t.reserved - t.sold) > 0
    ORDER BY e.starts_at ASC;