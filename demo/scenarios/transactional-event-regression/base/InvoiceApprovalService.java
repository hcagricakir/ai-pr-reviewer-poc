package com.acme.invoice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceApprovalService {

    private final InvoiceRepository invoiceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public InvoiceApprovalService(
            InvoiceRepository invoiceRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long invoiceId, Long approvedBy) throws InvoiceNotFoundException {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
        invoice.approve(approvedBy);
        invoiceRepository.save(invoice);
        eventPublisher.publishEvent(new InvoiceApprovedEvent(invoice.getId(), approvedBy));
    }
}
