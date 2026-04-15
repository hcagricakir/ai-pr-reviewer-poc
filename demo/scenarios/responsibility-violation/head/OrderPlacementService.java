package com.acme.order;

public class OrderPlacementService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;
    private final EmailNotifier emailNotifier;
    private final AuditPublisher auditPublisher;

    public OrderPlacementService(
            OrderRepository orderRepository,
            PaymentGateway paymentGateway,
            EmailNotifier emailNotifier,
            AuditPublisher auditPublisher
    ) {
        this.orderRepository = orderRepository;
        this.paymentGateway = paymentGateway;
        this.emailNotifier = emailNotifier;
        this.auditPublisher = auditPublisher;
    }

    public Order place(OrderCommand command) {
        validate(command);
        PaymentReceipt receipt = paymentGateway.charge(command.customerId(), command.totalAmount());
        Order order = orderRepository.save(Order.from(command, receipt.transactionId()));
        emailNotifier.sendEmail(command.customerEmail(), order.id());
        auditPublisher.publish(order.id(), "ORDER_CREATED");
        return order;
    }

    private void validate(OrderCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
    }
}
