package com.lanacash.switchmonetique.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lanacash.switchmonetique.services.MonetiqueSignatureService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class InternalApiTokenFilterTest {
    private static final String TOKEN = "internal-token";

    @Test
    void rejectsMissingTokenAndInvalidSignature() throws Exception {
        MonetiqueSignatureService signatures = mock(MonetiqueSignatureService.class);
        InternalApiTokenFilter filter = new InternalApiTokenFilter(TOKEN, signatures);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest missingToken = new MockHttpServletRequest("GET", "/api/switch/tpes");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(missingToken, firstResponse, chain);
        assertEquals(401, firstResponse.getStatus());

        MockHttpServletRequest badSignature = new MockHttpServletRequest("GET", "/api/switch/tpes");
        badSignature.addHeader("X-Monetique-Token", TOKEN);
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(badSignature, secondResponse, chain);
        assertEquals(401, secondResponse.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void acceptsAuthenticatedSignedRequestAndSkipsPublicPaths() throws Exception {
        MonetiqueSignatureService signatures = mock(MonetiqueSignatureService.class);
        when(signatures.verifyRequest(any(), any(), anyString(), anyString(), any())).thenReturn(true);
        InternalApiTokenFilter filter = new InternalApiTokenFilter(TOKEN, signatures);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest secured = new MockHttpServletRequest("GET", "/api/switch/tpes");
        secured.addHeader("X-Monetique-Token", TOKEN);
        filter.doFilter(secured, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());

        MockHttpServletRequest publicPath = new MockHttpServletRequest("GET", "/actuator/health");
        filter.doFilter(publicPath, new MockHttpServletResponse(), chain);
        verify(chain, times(2)).doFilter(any(), any());
    }
}
