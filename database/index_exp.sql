CREATE INDEX idx_reservations_pending_expires_at
ON reservations (expires_at)
WHERE status = 'PENDING';

