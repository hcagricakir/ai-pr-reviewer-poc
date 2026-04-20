package com.acme.order;

public class OrderEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrderEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishCreated(Order order) {
        eventPublisher.publishEvent(
                new KafkaEvent<>("order-created-prod", order.id(), new OrderCreatedEvent(order.id()))
        );
    }
}
