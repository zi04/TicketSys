package com.ticketflow.storage;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.id.EventId;
import io.vavr.collection.List;
import io.vavr.control.Option;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InMemoryEventRepository implements EventRepository {
    private final Map<EventId, Event> events = new LinkedHashMap<>();

    @Override
    public Option<Event> findById(EventId id) {
        return Option.of(events.get(id));
    }

    @Override
    public Event save(Event entity) {
        events.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public List<Event> findAll() {
        return List.ofAll(events.values());
    }
}
