package com.acme.client;

import java.time.LocalDateTime;

public class ClientActivationService {

    private final ClientRepository clientRepository;

    public ClientActivationService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public void updateActiveness(Long clientId, boolean active, Long updatedBy) throws ClientNotFoundException {
        int affectedRowCount = clientRepository.updateActiveness(clientId, active, updatedBy, LocalDateTime.now());
        if (affectedRowCount < 1) {
            throw new ClientNotFoundException(clientId);
        }
    }
}
