SELECT
    customer_id,

    SUM(total_amount) AS total_spent,
    'USD' AS currency
    FROM orders GROUP BY customer_id
    ORDER BY  total_spent DESC
    LIMIT 5;