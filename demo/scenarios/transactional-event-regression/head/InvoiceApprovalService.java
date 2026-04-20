package com.acme.invoice;

import org.springframework.stereotype.Service;

@Service
public class InvoiceApprovalService {

    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountingNotifier accountingNotifier;

    public InvoiceApprovalService(
            InvoiceRepository invoiceRepository,
            ApplicationEventPublisher eventPublisher,
            AccountingNotifier accountingNotifier
    ) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
        this.accountingNotifier = accountingNotifier;
    }

    public void approve(Long invoiceId, Long approvedBy) throws InvoiceNotFoundException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        invoice.approve(approvedBy);
        eventPublisher.publishEvent(new InvoiceApprovedEvent(invoice.getId(), approvedBy));
        accountingNotifier.notifyApproved(invoice.getId());
        invoiceRepository.save(invoice);
    }
}
