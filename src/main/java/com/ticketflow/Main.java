package com.ticketflow;

import com.ticketflow.storage.EventRepository;
import com.ticketflow.storage.InMemoryEventRepository;
import com.ticketflow.storage.InMemoryOrderRepository;
import com.ticketflow.storage.InMemoryReservationRepository;
import com.ticketflow.storage.OrderRepository;
import com.ticketflow.storage.ReservationRepository;

public final class Main {
    public static void main(String[] args) {
        EventRepository eventRepository = new InMemoryEventRepository();
        ReservationRepository reservationRepository = new InMemoryReservationRepository();
        OrderRepository orderRepository = new InMemoryOrderRepository();
        TicketFlowManager ticketFlow = new TicketFlowManager(
                eventRepository, reservationRepository, orderRepository);

        new Cli(ticketFlow).run();
    }
}
