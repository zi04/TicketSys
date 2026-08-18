package com.ticketflow.storage;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.id.EventId;

public interface EventRepository extends Repository<Event, EventId> {
}
