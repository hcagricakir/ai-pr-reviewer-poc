package com.acme.client;

import com.clovify.extra.spring.rest.ServiceResponse;
import com.clovify.extra.spring.rest.ServiceResponses;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/{id}")
    public ServiceResponse<ClientDto> getById(@PathVariable @Positive Long id) {
        return ServiceResponses.successful(clientService.getById(id));
    }
}
