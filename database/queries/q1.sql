\set event_id '11111111-1111-1111-1111-111111111111'

SELECT name AS
    ticket_type,
    allocation,
    reserved,
    sold,
    available
    FROM ticket_availability
    WHERE event_id = :'event_id'::uuid
    ORDER BY name;