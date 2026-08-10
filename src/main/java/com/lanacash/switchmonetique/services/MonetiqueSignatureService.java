package com.lanacash.switchmonetique.services;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MonetiqueSignatureService {
    private final byte[] secret;
    private final Map<String, Long> processedRequests = new ConcurrentHashMap<>();
    private static final long MAX_AGE_SECONDS = 300;

    public MonetiqueSignatureService(@Value("${app.demo.signature-secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("MONETIQUE_SIGNATURE_SECRET doit contenir au moins 32 caractères.");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public SignedEvent sign(TransactionMonetique transaction) {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String eventId = transaction.getIdTransaction();
        return new SignedEvent(timestamp, eventId, hmac(timestamp + "." + eventId + "." + canonical(transaction)));
    }

    public boolean verifyRequest(String timestampValue, String requestId, String method, String path,
                                 String suppliedSignature) {
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampValue);
        } catch (RuntimeException exception) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > MAX_AGE_SECONDS || requestId == null || suppliedSignature == null) {
            return false;
        }
        processedRequests.entrySet().removeIf(entry -> now - entry.getValue() > MAX_AGE_SECONDS);
        if (processedRequests.putIfAbsent(requestId, now) != null) {
            return false;
        }
        String expected = hmac(timestampValue + "." + requestId + "." + method + "." + path);
        boolean valid = MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.US_ASCII), suppliedSignature.getBytes(StandardCharsets.US_ASCII)
        );
        if (!valid) processedRequests.remove(requestId);
        return valid;
    }

    private String canonical(TransactionMonetique t) {
        return String.join("|", value(t.getIdTransaction()), value(t.getCanal()), value(t.getIdTpe()),
            value(t.getIdSiteEcommerce()), value(t.getIdCommercant()), value(t.getMontant()), value(t.getDevise()),
            value(t.getTypeTransaction()), value(t.getCodeReponse()), value(t.getStatut()), value(t.getDateTransaction()));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Calcul HMAC impossible.", exception);
        }
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
    public record SignedEvent(String timestamp, String eventId, String signature) { }
}
