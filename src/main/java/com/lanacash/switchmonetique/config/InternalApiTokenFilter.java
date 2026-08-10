package com.lanacash.switchmonetique.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.lanacash.switchmonetique.services.MonetiqueSignatureService;

/** Ferme les API REST monétiques aux appels qui ne proviennent pas des services autorisés. */
@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {
    private final byte[] expectedToken;
    private final MonetiqueSignatureService signatureService;

    public InternalApiTokenFilter(@Value("${app.demo.internal-token}") String token,
                                  MonetiqueSignatureService signatureService) {
        this.expectedToken = token.getBytes(StandardCharsets.UTF_8);
        this.signatureService = signatureService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/switch/") || path.startsWith("/api/ecommerce/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String supplied = request.getHeader("X-Monetique-Token");
        if (supplied == null || !MessageDigest.isEqual(expectedToken, supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentification inter-service requise");
            return;
        }
        if (!signatureService.verifyRequest(
                request.getHeader("X-Monetique-Timestamp"),
                request.getHeader("X-Monetique-Request-Id"),
                request.getMethod(), request.getRequestURI(),
                request.getHeader("X-Monetique-Signature"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Signature invalide ou requête rejouée");
            return;
        }
        chain.doFilter(request, response);
    }
}
