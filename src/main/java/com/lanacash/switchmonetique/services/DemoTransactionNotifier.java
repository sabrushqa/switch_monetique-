package com.lanacash.switchmonetique.services;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Envoie une projection de la transaction à demo sans lui donner accès à Oracle. */
@Component
public class DemoTransactionNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoTransactionNotifier.class);

    private final RestClient restClient;
    private final String internalToken;
    private final MonetiqueSignatureService signatureService;

    public DemoTransactionNotifier(
            RestClient.Builder builder,
            @Value("${app.demo.base-url:http://localhost:8000}") String demoBaseUrl,
            @Value("${app.demo.internal-token:change-me-monetique-internal-token}") String internalToken,
            MonetiqueSignatureService signatureService
    ) {
        this.restClient = builder.baseUrl(stripTrailingSlash(demoBaseUrl)).build();
        this.internalToken = internalToken;
        this.signatureService = signatureService;
    }

    public void notifyDemo(TransactionMonetique transaction) {
        try {
            MonetiqueSignatureService.SignedEvent signed = signatureService.sign(transaction);
            restClient.post()
                    .uri("/api/internal/monetique/transactions")
                    .header("X-Monetique-Token", internalToken)
                    .header("X-Monetique-Timestamp", signed.timestamp())
                    .header("X-Monetique-Event-Id", signed.eventId())
                    .header("X-Monetique-Signature", signed.signature())
                    .body(transaction)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("demo a refusé le webhook: " + response.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (RuntimeException exception) {
            // La décision monétique reste valide et persistée dans Oracle. Une synchronisation
            // REST peut rejouer la projection grâce à l'identifiant idempotent de transaction.
            LOGGER.warn("Notification de la transaction {} vers demo impossible: {}",
                    transaction.getIdTransaction(), exception.getMessage());
        }
    }

    private String stripTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
