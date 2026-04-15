package com.acme.order;

public class OrderPlacementService {

    private final OrderRepository orderRepository;

    public OrderPlacementService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order place(OrderCommand command) {
        validate(command);
        return orderRepository.save(Order.from(command));
    }

    private void validate(OrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
    }
}
