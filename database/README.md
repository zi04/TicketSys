# TicketFlow — Week 2: The Database

**Branch:** `week-02-database`  
**Database:** PostgreSQL 18  
**Interface:** `psql`  
**Runtime:** Docker  

## 1. Overview

This week implements the TicketFlow persistence layer entirely in PostgreSQL. The Week 1 Java application remains untouched. The database contains the TicketFlow schema, constraints, story seed, generated volume seed, query catalogue, transaction scripts, indexing work, and the required concurrency experiments.

The implementation is designed so the database can be recreated from an empty PostgreSQL instance by running the SQL files in order.

## 2. Project layout

```text
database/
├── 1_schema.sql
├── 2_story_seed.sql
├── 3_generator_seed.sql
├── index_exp.sql
├── queries/
│   ├── q1.sql
│   ├── q2.sql
│   ├── q3.sql
│   ├── q4.sql
│   ├── q5.sql
│   ├── q6.sql
│   ├── q7.sql
│   └── q8.sql
└── transactions/
    ├── reserve.sql
    ├── pay.sql
    ├── cancel.sql
    └── expire.sql
```

## 3. Start PostgreSQL 18

Create the local PostgreSQL container:

```bash
docker run --name ticketflow-db \
  -e POSTGRES_PASSWORD=ticketflow \
  -p 5432:5432 \
  -d postgres:18
```

If the container already exists but is stopped:

```bash
docker start ticketflow-db
```

Connect to PostgreSQL:

```bash
docker exec -it ticketflow-db psql -U postgres
```

## 4. Rebuild the database from empty

The following is the exact rebuild sequence used for TicketFlow.

### 4.1 Drop and recreate the database

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d postgres \
  -c "DROP DATABASE IF EXISTS ticketflow WITH (FORCE);"

docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d postgres \
  -c "CREATE DATABASE ticketflow;"
```

### 4.2 Build the schema

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/1_schema.sql
```

### 4.3 Load the hand-written story seed

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/2_story_seed.sql
```

### 4.4 Load the generated volume seed

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/3_generator_seed.sql
```

### 4.5 Add the measured performance index

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/index_exp.sql
```

The query and transaction files are executable scripts rather than build files, so they are run separately when needed.

## 5. Schema design

The database contains six main tables:

```text
events
├── ticket_types
└── reservations
    ├── reservation_lines
    ├── payments
    └── orders
```

### Tables

- `events` stores organisers, event names, sales windows, start times, and event status.
- `ticket_types` stores ticket prices, allocation, per-customer limits, `reserved`, and `sold` counters.
- `reservations` stores customer holds and their status/expiry.
- `reservation_lines` stores the ticket snapshot, price, currency, and quantity belonging to each reservation.
- `payments` stores every payment attempt with `APPROVED` or `DECLINED` status.
- `orders` stores completed purchases.

UUIDs are used for business IDs. TicketFlow currently uses USD in its provided seeds and transaction scripts. Monetary values use exact `NUMERIC(12,2)` values and keep the currency beside the amount.

## 6. D1–D10 persistence rules

| Rule | Implementation |
|---|---|
| **D1** | `events`, `ticket_types`, `reservations`, `reservation_lines`, `payments`, and `orders` store the Week 1 business data. Seat counters are stored as `reserved` and `sold`. |
| **D2** | `1_schema.sql`, `2_story_seed.sql`, and `3_generator_seed.sql` rebuild the database in order from an empty `ticketflow` database. `ON_ERROR_STOP=1` is used during the rebuild. |
| **D3** | Available seats are never stored. `ticket_availability` calculates `allocation - reserved - sold`. |
| **D4** | CHECK constraints and enums reject negative prices/counters, overselling, invalid quantities, invalid statuses, and invalid event sales windows. |
| **D5** | `un_approved_payment` prevents the same payment reference being approved more than once. `orders.reservation_id` is unique, so one reservation can create at most one order. |
| **D6** | Money uses `NUMERIC(12,2)` plus a currency column. Business timestamps use `TIMESTAMPTZ`, and the schema sets the session time zone to UTC. |
| **D7** | `reserve.sql`, `pay.sql`, `cancel.sql`, and `expire.sql` each use one `BEGIN` → `COMMIT`/`ROLLBACK` transaction. |
| **D8** | `2_story_seed.sql` is the readable hand-written seed. `3_generator_seed.sql` uses `generate_series` to create performance-scale data. |
| **D9** | `idx_reservations_pending_expires_at` was added only after measuring Q7 before and after. `pending_customer` and `un_approved_payment` are correctness indexes that enforce business uniqueness. |
| **D10** | Scripts that depend on the current time receive it through a `psql` variable such as `\set current_time ...`; they do not call `now()`. |

### Important database constraints

The schema includes, among others:

```text
check_event_sales
check_ticket_price
check_ticket_reserved
check_ticket_sold
check_ticket_threshold
check_reservation_line_quantity
pending_customer
un_approved_payment
UNIQUE orders.reservation_id
```

The composite foreign key on `reservation_lines(event_id, ticket_id)` ensures that a reservation line cannot reference a ticket type belonging to a different event.

The partial unique index:

```sql
CREATE UNIQUE INDEX pending_customer
ON reservations(customer_id, event_id)
WHERE status = 'PENDING';
```

ensures that a customer cannot hold two `PENDING` reservations for the same event.

## 7. Seed data

### 7.1 Story seed

`2_story_seed.sql` contains readable examples of the important TicketFlow states:

- a published event;
- a draft event;
- an event that is 90% sold;
- `PENDING`, `CONFIRMED`, `CANCELLED`, and `EXPIRED` reservations;
- a `PENDING` reservation whose expiry has already passed;
- a declined payment;
- the same payment reference retried and approved;
- completed orders.

The retry story is represented by:

```text
PAY-RETRY-001  DECLINED
PAY-RETRY-001  APPROVED
```

This keeps payment-attempt history while the partial unique index prevents a second `APPROVED` use of the same reference.

### 7.2 Generated volume seed

`3_generator_seed.sql` uses `generate_series` to generate:

- 5,000 generated events;
- 5,000 generated ticket types;
- 250,000 generated reservations;
- 250,000 generated reservation lines;
- 60,000 generated approved payments;
- 60,000 generated orders.

Together with the story seed, the deterministic final counts are approximately:

| Table | Rows after both seeds |
|---|---:|
| `events` | 5,003 |
| `ticket_types` | 5,004 |
| `reservations` | 250,006 |
| `reservation_lines` | 250,006 |
| `payments` | 60,003 |
| `orders` | 60,002 |

The large seed exists specifically so realistic catalogue queries can be measured with `EXPLAIN ANALYZE`.

# 8. Query catalogue

The sample rows below are shown from the small story dataset so the results remain easy to read. After loading the volume seed, the same queries operate on the full generated dataset.

Run any catalogue query with:

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/queries/q1.sql
```

Replace `q1.sql` with the required query file.

## Q1 — Ticket availability for one event

**Question:** For one event, show every ticket type with allocation, reserved, sold, and available.

```sql
\set event_id '11111111-1111-1111-1111-111111111111'

SELECT name AS ticket_type,
       allocation,
       reserved,
       sold,
       available
FROM ticket_availability
WHERE event_id = :'event_id'::uuid
ORDER BY name;
```

Example output:

| ticket_type | allocation | reserved | sold | available |
|---|---:|---:|---:|---:|
| General | 100 | 3 | 2 | 95 |
| VIP | 20 | 0 | 0 | 20 |

## Q2 — Published events that still have seats

**Question:** Show all published events that still have seats, soonest first.

```sql
SELECT e.id,
       e.name,
       e.starts_at,
       SUM(t.allocation - t.reserved - t.sold) AS available
FROM events e
JOIN ticket_types t ON t.event_id = e.id
WHERE e.status = 'PUBLISHED'
GROUP BY e.id, e.name, e.starts_at
HAVING SUM(t.allocation - t.reserved - t.sold) > 0
ORDER BY e.starts_at ASC;
```

Example output:

| name | starts_at | available |
|---|---|---:|
| test event 1 | 2026-09-11 16:00:00+00 | 115 |
| Backend Workshop | 2026-09-20 16:00:00+00 | 1 |

## Q3 — One customer's reservations

**Question:** Show one customer's reservations newest first, including status and total.

```sql
\set customer_id 'customer-2'

SELECT r.id AS reservation_id,
       e.name AS event,
       r.status,
       SUM(rl.price * rl.quantity) AS total_amount,
       'USD' AS currency,
       r.created_at
FROM reservations r
JOIN events e ON e.id = r.event_id
JOIN reservation_lines rl ON rl.reservation_id = r.id
WHERE r.customer_id = :'customer_id'
GROUP BY r.id, e.name, r.status, r.created_at
ORDER BY r.created_at DESC;
```

Example output:

| event | status | total_amount | currency |
|---|---|---:|---|
| test event 1 | CONFIRMED | 50.00 | USD |

## Q4 — Revenue per event and ticket type

**Question:** Show revenue per event and, within each event, revenue per ticket type.

Per event:

```sql
SELECT e.id AS event_id,
       e.name AS event,
       SUM(o.total_amount) AS revenue,
       'USD' AS currency
FROM events e
JOIN orders o ON o.event_id = e.id
GROUP BY e.id, e.name
ORDER BY revenue DESC;
```

Example output:

| event | revenue | currency |
|---|---:|---|
| Backend Workshop | 180.00 | USD |
| test event 1 | 50.00 | USD |

Per ticket type:

```sql
SELECT e.id AS event_id,
       e.name AS event,
       tt.id AS ticket_id,
       tt.name AS ticket_name,
       SUM(rl.price * rl.quantity) AS revenue,
       'USD' AS currency
FROM orders o
JOIN events e ON e.id = o.event_id
JOIN reservation_lines rl ON rl.reservation_id = o.reservation_id
JOIN ticket_types tt ON tt.id = rl.ticket_id
GROUP BY e.id, e.name, tt.id, tt.name
ORDER BY e.name, revenue DESC;
```

Example output:

| event | ticket_name | revenue |
|---|---|---:|
| Backend Workshop | Standard | 180.00 |
| test event 1 | General | 50.00 |

## Q5 — Top five customers

**Question:** Show the five customers who have spent the most.

```sql
SELECT customer_id,
       SUM(total_amount) AS total_spent,
       'USD' AS currency
FROM orders
GROUP BY customer_id
ORDER BY total_spent DESC
LIMIT 5;
```

Example output from the story seed:

| customer_id | total_spent | currency |
|---|---:|---|
| customer-6 | 180.00 | USD |
| customer-2 | 50.00 | USD |

## Q6 — Events more than 80% sold

**Question:** Show events whose ticket allocation is more than 80% sold.

```sql
SELECT e.id AS event_id,
       e.name AS event,
       SUM(tt.allocation) AS total_allocation,
       SUM(tt.sold) AS total_sold,
       (SUM(tt.sold)::NUMERIC / SUM(tt.allocation)) * 100 AS sold_percentage
FROM events e
JOIN ticket_types tt ON tt.event_id = e.id
GROUP BY e.id, e.name
HAVING SUM(tt.sold)::NUMERIC / SUM(tt.allocation) > 0.80
ORDER BY sold_percentage DESC;
```

Example output:

| event | total_allocation | total_sold | sold_percentage |
|---|---:|---:|---:|
| Backend Workshop | 10 | 9 | 90.00% |

## Q7 — Expired pending reservations

**Question:** Show every `PENDING` reservation whose expiry time has already passed.

```sql
\set current_time '2026-08-30 12:10:00+00'

SELECT r.id AS reservation_id,
       r.customer_id,
       e.name AS event,
       r.created_at,
       r.expires_at
FROM reservations r
JOIN events e ON e.id = r.event_id
WHERE r.status = 'PENDING'
  AND r.expires_at < :'current_time'::timestamptz
ORDER BY r.expires_at ASC;
```

Example output:

| customer_id | event | expires_at |
|---|---|---|
| customer-5 | test event 1 | 2026-08-29 09:15:00+00 |
| customer-1 | test event 1 | 2026-08-30 12:05:00+00 |

The current time is supplied as a `psql` variable rather than using `now()`, satisfying D10.

## Q8 — Retried payment references

**Question:** Show payment references attempted more than once.

```sql
SELECT payment_reference,
       COUNT(*) AS attempt_count
FROM payments
GROUP BY payment_reference
HAVING COUNT(*) > 1
ORDER BY attempt_count DESC;
```

Example output:

| payment_reference | attempt_count |
|---|---:|
| PAY-RETRY-001 | 2 |

# 9. Transaction scripts

All four business operations are implemented as one database transaction. Each script takes its test inputs through `psql` `\set` variables.

## R9–R15 mapping

| Week 1 rule | SQL implementation |
|---|---|
| **R9** Reserving is all or nothing | `transactions/reserve.sql` begins a transaction, locks the ticket row, checks availability, creates the reservation/line, updates `reserved`, and commits. Failure rolls back the transaction. |
| **R10** Reservation holds seats for 15 minutes | `reserve.sql` sets `expires_at = current_time + INTERVAL '15 minutes'`. `expire.sql` processes expired holds. |
| **R11** Only the owner of a pending, unexpired reservation may pay/cancel | `pay.sql` and `cancel.sql` check `customer_id`, `status = 'PENDING'`, and `expires_at > current_time`. |
| **R12** Cancel/expire returns seats | `cancel.sql` and `expire.sql` subtract each reservation-line quantity from `ticket_types.reserved`. |
| **R13** Payment amount equals reservation total | `pay.sql` calculates `SUM(price * quantity)` and compares it exactly with the supplied amount. TicketFlow transaction data uses USD. |
| **R14** Successful payment moves reserved → sold, confirms, and creates order | `pay.sql` decreases `reserved`, increases `sold`, sets reservation status to `CONFIRMED`, inserts the payment, and inserts the order in the same transaction. |
| **R15** Reusing a successful payment reference does not pay twice | `pay.sql` first looks for an existing approved payment/order with that reference and returns the existing order. `un_approved_payment` also prevents a second approved use at the database level. |

### Run a transaction

Example:

```bash
docker exec -i ticketflow-db \
  psql -v ON_ERROR_STOP=1 -U postgres -d ticketflow \
  < database/transactions/reserve.sql
```

The values at the top of each script can be changed through its `\set` inputs before running a test scenario.

# 10. Rollback demonstration

R9 requires a reservation to be all-or-nothing. PostgreSQL transaction rollback was also tested deliberately by making a transaction fail after a write.

Example reproduction:

```sql
BEGIN;

SELECT reserved
FROM ticket_types
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

UPDATE ticket_types
SET reserved = reserved + 1
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

-- Intentional failure while the transaction is still open.
SELECT 1 / 0;

ROLLBACK;

SELECT reserved
FROM ticket_types
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
```

The final `reserved` value is the same as before the transaction, demonstrating that an operation which fails midway does not leave a partial seat hold behind.

Closing a `psql` session while an uncommitted transaction is open produces the same principle: PostgreSQL rolls the transaction back when the connection ends.

# 11. Experiments

## 11.1 Experiment 5.1 — Make a slow query fast

Q7 was selected because it searches the large `reservations` table for `PENDING` rows before a supplied expiry time.

Test query:

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT r.id,
       r.customer_id,
       r.expires_at
FROM reservations r
WHERE r.status = 'PENDING'
  AND r.expires_at < '2026-08-30 12:00:00+00'::timestamptz
ORDER BY r.expires_at;
```

### Before the index

```text
Gather Merge  (cost=5853.61..6823.43 rows=8327 width=41) (actual time=14.133..17.264 rows=8321.00 loops=1)
  Workers Planned: 2
  Workers Launched: 2
  Buffers: shared hit=3161
  ->  Sort  (cost=4853.59..4862.26 rows=3470 width=41) (actual time=8.858..8.933 rows=2773.67 loops=3)
        Sort Key: expires_at
        Sort Method: quicksort  Memory: 351kB
        Buffers: shared hit=3161
        Worker 0:  Sort Method: quicksort  Memory: 230kB
        Worker 1:  Sort Method: quicksort  Memory: 228kB
        ->  Parallel Seq Scan on reservations r  (cost=0.00..4649.54 rows=3470 width=41) (actual time=0.332..7.907 rows=2773.67 loops=3)
              Filter: ((expires_at < '2026-08-30 12:00:00'::timestamp without time zone) AND (status = 'PENDING'::reservation_status))
              Rows Removed by Filter: 80562
              Buffers: shared hit=3087
Planning:
  Buffers: shared hit=38 dirtied=1
Planning Time: 0.597 ms
Execution Time: 17.589 ms
```

The important part of the original plan was the **Parallel Sequential Scan** over `reservations`.

The measured index was then added:

```sql
CREATE INDEX idx_reservations_pending_expires_at
ON reservations (expires_at)
WHERE status = 'PENDING';
```

This is stored in `database/index_exp.sql`.

### After the index

```text
Sort  (cost=3854.97..3875.78 rows=8327 width=41) (actual time=5.676..6.009 rows=8321.00 loops=1)
  Sort Key: expires_at
  Sort Method: quicksort  Memory: 904kB
  Buffers: shared hit=115 read=10
  ->  Bitmap Heap Scan on reservations r  (cost=100.82..3312.73 rows=8327 width=41) (actual time=0.673..2.758 rows=8321.00 loops=1)
        Recheck Cond: ((expires_at < '2026-08-30 12:00:00'::timestamp without time zone) AND (status = 'PENDING'::reservation_status))
        Heap Blocks: exact=115
        Buffers: shared hit=115 read=10
        ->  Bitmap Index Scan on idx_reservations_pending_expires_at  (cost=0.00..98.74 rows=8327 width=0) (actual time=0.600..0.600 rows=8321.00 loops=1)
              Index Cond: (expires_at < '2026-08-30 12:00:00'::timestamp without time zone)
              Index Searches: 1
              Buffers: shared read=10
Planning:
  Buffers: shared hit=69 read=1
Planning Time: 0.909 ms
Execution Time: 6.410 ms
```

### Result

| Measurement | Before | After |
|---|---:|---:|
| Execution time | 17.589 ms | 6.410 ms |
| Main access method | Parallel Seq Scan | Bitmap Index Scan + Bitmap Heap Scan |
| Rows returned | 8,321 | 8,321 |

The execution time fell by approximately **63.6%**, making the measured query about **2.74× faster**.

**Index cost:** the index consumes additional disk space and adds write overhead because PostgreSQL must maintain it whenever relevant reservation rows are inserted or their indexed state changes.

## 11.2 Experiment 5.2 — Two terminals, one row

A test ticket row was set to:

```text
reserved = 10
```

### Without `FOR UPDATE`

Session A:

```sql
BEGIN;
SELECT reserved FROM ticket_types WHERE id = '<test-ticket-id>';
-- result: 10
```

Session B:

```sql
BEGIN;
SELECT reserved FROM ticket_types WHERE id = '<test-ticket-id>';
-- result: 10
```

Both sessions calculated `10 + 2 = 12`.

Session A:

```sql
UPDATE ticket_types SET reserved = 12 WHERE id = '<test-ticket-id>';
COMMIT;
```

Session B:

```sql
UPDATE ticket_types SET reserved = 12 WHERE id = '<test-ticket-id>';
COMMIT;
```

Final result:

```text
expected: 14
actual:   12
```

No PostgreSQL error occurred. One update was silently lost because both transactions read the same old value and later wrote the same calculated value.

### With `SELECT ... FOR UPDATE`

The counter was reset to `10`.

Session A:

```sql
BEGIN;
SELECT reserved
FROM ticket_types
WHERE id = '<test-ticket-id>'
FOR UPDATE;
-- result: 10
```

Session B then executed the same `SELECT ... FOR UPDATE` and **blocked** because Session A held the row lock.

Session A updated the value to `12` and committed. Session B then unblocked and saw the new value `12`. It applied its own `+2`, committed, and the final counter became:

```text
14
```

### Conclusion

`reserve.sql` protects its ticket row with `FOR UPDATE` before checking availability and changing the counter, so it does not perform the unsafe read-calculate-write pattern demonstrated in the first half of the experiment.

A single SQL statement such as:

```sql
UPDATE ticket_types
SET reserved = reserved + 2
WHERE id = ...;
```

is itself safe from this specific lost-update pattern because PostgreSQL obtains a row lock for the update. A concurrent update waits and then applies its expression to the latest row version. TicketFlow still needs its explicit availability checks to prevent overselling.

## 11.3 Experiment 5.3 — Deadlock

Two sessions intentionally locked two ticket rows in opposite order.

Initial lock order:

```text
Session A locks Ticket A
Session B locks Ticket B
```

Session A then requested Ticket B and waited. Session B requested Ticket A, creating a circular wait:

```text
Session A owns A → waits for B
Session B owns B → waits for A
```

PostgreSQL detected the cycle and aborted one transaction so that the other could continue.

Actual PostgreSQL deadlock details captured during the experiment:

```text
DETAIL:  Process 2441 waits for ShareLock on transaction 980; blocked by process 2433.
Process 2433 waits for ShareLock on transaction 981; blocked by process 2441.
HINT:  See server log for query details.
CONTEXT:  while locking tuple (0,1) in relation "ticket_types"
```

The aborted transaction enters the failed transaction state and is cleaned up with:

```sql
ROLLBACK;
```

### Why PostgreSQL aborts one transaction

If PostgreSQL left both transactions waiting, neither could ever make progress. Aborting one transaction releases its locks and allows the other transaction to continue.

### Application behaviour

An application receiving a deadlock error should roll back the failed transaction and retry the complete business operation. Locking multiple rows in a consistent order also reduces the chance of creating deadlocks.

# 12. Database rejection checks

The database itself protects the important invalid states rather than relying only on application code.

### Negative counter

```sql
UPDATE ticket_types
SET reserved = -1
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
```

Rejected by `check_ticket_reserved`.

### Overselling

```sql
UPDATE ticket_types
SET reserved = 95,
    sold = 10
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
```

For an allocation of 100, this attempts to account for 105 seats and is rejected by `check_ticket_threshold`.

### Negative price

```sql
UPDATE ticket_types
SET price = -1
WHERE id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
```

Rejected by `check_ticket_price`.

### Invalid status

```sql
UPDATE reservations
SET status = 'UNKNOWN'
WHERE id = '10000000-0000-0000-0000-000000000001';
```

Rejected because `UNKNOWN` is not a valid `reservation_status` enum value.

### Duplicate successful payment reference

The schema contains:

```sql
CREATE UNIQUE INDEX un_approved_payment
ON payments(payment_reference)
WHERE status = 'APPROVED';
```

A declined reference may be retried, but a second `APPROVED` use of the same reference is rejected.

### Second order for one reservation

`orders.reservation_id` is declared `UNIQUE`, so a second order for the same reservation is rejected by PostgreSQL.

# 13. Submission checklist

- [x] PostgreSQL 18 Docker command documented.
- [x] Exact rebuild sequence documented.
- [x] Schema created by hand-written SQL.
- [x] Week 1 Java application remains untouched for Week 2.
- [x] Tables cover events, ticket types, counters, reservations, lines, payments, and orders.
- [x] Availability is calculated rather than stored.
- [x] Business constraints are enforced by PostgreSQL.
- [x] Exact decimal money and currency columns are used.
- [x] Business timestamps are stored as `TIMESTAMPTZ` in UTC.
- [x] Story seed includes every reservation status, expired hold, declined payment, retried reference, and paid orders.
- [x] Generated seed creates thousands of events and hundreds of thousands of reservations.
- [x] Q1–Q8 query catalogue is present.
- [x] `reserve.sql`, `pay.sql`, `cancel.sql`, and `expire.sql` are transactional.
- [x] Current time is supplied as a `psql` variable where needed.
- [x] Rollback behaviour is documented.
- [x] Indexing experiment includes real before/after plans and timings.
- [x] Lost-update and row-locking experiment is documented.
- [x] Deadlock experiment and recovery behaviour are documented.
- [x] Performance-index cost is explained.

