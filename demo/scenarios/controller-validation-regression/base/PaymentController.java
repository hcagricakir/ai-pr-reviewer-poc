package com.acme.payment;

import com.clovify.extra.spring.rest.ServiceResponse;
import com.clovify.extra.spring.rest.ServiceResponses;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentApprovalService paymentApprovalService;

    public PaymentController(PaymentApprovalService paymentApprovalService) {
        this.paymentApprovalService = paymentApprovalService;
    }

    @PostMapping("/approve")
    public ServiceResponse approve(
            @RequestBody @Validated ApprovePaymentRequest request,
            @RequestParam @NotNull @Positive Long approverId
    ) {
        paymentApprovalService.approve(request, approverId);
        return ServiceResponses.successful();
    }
}
