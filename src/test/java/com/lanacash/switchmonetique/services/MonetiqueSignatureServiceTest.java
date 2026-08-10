package com.lanacash.switchmonetique.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.enums.CanalTransaction;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

class MonetiqueSignatureServiceTest {
    private static final String SECRET = "switch-test-signature-secret-at-least-32-chars";

    @Test
    void acceptsValidCommandAndRejectsReplayAndTampering() throws Exception {
        MonetiqueSignatureService service = new MonetiqueSignatureService(SECRET);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();
        String canonical = timestamp + "." + requestId + ".PUT./api/switch/tpes/TPE-1/assign";
        String signature = hmac(canonical);

        assertTrue(service.verifyRequest(timestamp, requestId, "PUT", "/api/switch/tpes/TPE-1/assign", signature));
        assertFalse(service.verifyRequest(timestamp, requestId, "PUT", "/api/switch/tpes/TPE-1/assign", signature));
        assertFalse(service.verifyRequest(timestamp, UUID.randomUUID().toString(), "DELETE",
            "/api/switch/tpes/TPE-1/assign", signature));
    }

    @Test
    void rejectsExpiredSignature() {
        MonetiqueSignatureService service = new MonetiqueSignatureService(SECRET);
        assertFalse(service.verifyRequest("1", UUID.randomUUID().toString(), "GET", "/api/switch/tpes", "bad"));
        assertFalse(service.verifyRequest("not-a-time", UUID.randomUUID().toString(), "GET", "/api/switch/tpes", "bad"));
        assertFalse(service.verifyRequest(String.valueOf(Instant.now().getEpochSecond()), null,
            "GET", "/api/switch/tpes", "bad"));
        assertFalse(service.verifyRequest(String.valueOf(Instant.now().getEpochSecond()),
            UUID.randomUUID().toString(), "GET", "/api/switch/tpes", null));
        assertThrows(IllegalStateException.class, () -> new MonetiqueSignatureService("short"));
    }

    @Test
    void allowsRetryAfterAnInvalidSignature() throws Exception {
        MonetiqueSignatureService service = new MonetiqueSignatureService(SECRET);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String requestId = UUID.randomUUID().toString();
        String canonical = timestamp + "." + requestId + ".GET./api/switch/tpes";
        assertFalse(service.verifyRequest(timestamp, requestId, "GET", "/api/switch/tpes", "bad"));
        assertTrue(service.verifyRequest(timestamp, requestId, "GET", "/api/switch/tpes", hmac(canonical)));
    }

    @Test
    void signsOutgoingTransactionWebhook() {
        MonetiqueSignatureService service = new MonetiqueSignatureService(SECRET);
        TransactionMonetique transaction = new TransactionMonetique();
        transaction.setIdTransaction("tx-signed");
        transaction.setCanal(CanalTransaction.TPE);
        transaction.setIdTpe("TPE-1");
        transaction.setIdCommercant("42");
        transaction.setMontant(new BigDecimal("10.00"));
        transaction.setDevise("MAD");
        transaction.setTypeTransaction(TypeTransaction.ACHAT);
        transaction.setCodeReponse("00");
        transaction.setStatut(StatutTransaction.APPROVED);
        transaction.setDateTransaction(LocalDateTime.of(2026, 8, 10, 12, 0));

        MonetiqueSignatureService.SignedEvent signed = service.sign(transaction);
        assertTrue(signed.timestamp().matches("\\d+"));
        assertTrue(signed.eventId().equals("tx-signed"));
        assertTrue(signed.signature().matches("[0-9a-f]{64}"));
    }

    private String hmac(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
