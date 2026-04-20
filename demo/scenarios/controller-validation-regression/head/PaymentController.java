package com.acme.payment;

import com.clovify.extra.spring.rest.ServiceResponse;
import com.clovify.extra.spring.rest.ServiceResponses;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentApprovalService paymentApprovalService;

    public PaymentController(PaymentApprovalService paymentApprovalService) {
        this.paymentApprovalService = paymentApprovalService;
    }

    @PostMapping("/approve")
    public ServiceResponse approve(
            @RequestBody Map<String, Object> request,
            @RequestParam Long approverId
    ) {
        paymentApprovalService.approve(request, approverId);
        return ServiceResponses.successful();
    }
}
