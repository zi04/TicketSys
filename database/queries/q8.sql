SELECT
    payment_reference,
    COUNT(*) AS attempt_count FROM payments
    GROUP BY payment_reference HAVING COUNT (*) > 1
    ORDER BY attempt_count DESC;