package com.lanacash.switchmonetique.iso8583;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.services.AuthorizationService;
import java.math.BigDecimal;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TpeRequestListenerTest {

    @Test
    void translatesIso0200IntoApproved0210() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        TpeRequestListener listener = new TpeRequestListener(service);
        ISOSource source = mock(ISOSource.class);
        TransactionMonetique approved = new TransactionMonetique();
        approved.setStatut(StatutTransaction.APPROVED);
        approved.setCodeReponse("00");
        approved.setCodeAutorisation("123456");
        when(service.autoriserTransactionTpe(eq("TPE-001"), eq(new BigDecimal("125.50")), any(),
            eq("000001"), eq("000001"), eq(true), eq(false))).thenReturn(approved);

        ISOMsg request = new ISOMsg();
        request.setMTI("0200");
        request.set(2, "4111111111111111");
        request.set(4, "000000012550");
        request.set(11, "000001");
        request.set(41, "TPE-001");

        assertTrue(listener.process(source, request));
        ArgumentCaptor<ISOMsg> responseCaptor = ArgumentCaptor.forClass(ISOMsg.class);
        verify(source).send(responseCaptor.capture());
        assertEquals("0210", responseCaptor.getValue().getMTI());
        assertEquals("00", responseCaptor.getValue().getString(39));
        assertEquals("123456", responseCaptor.getValue().getString(38));
    }

    @Test
    void ignoresUnsupportedOrMalformedMessages() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        TpeRequestListener listener = new TpeRequestListener(service);
        ISOSource source = mock(ISOSource.class);
        ISOMsg networkMessage = new ISOMsg();
        networkMessage.setMTI("0800");
        assertFalse(listener.process(source, networkMessage));

        ISOMsg malformed = new ISOMsg();
        malformed.setMTI("0200");
        malformed.set(4, "not-a-number");
        assertFalse(listener.process(source, malformed));
        verifyNoInteractions(service);
    }

    @Test
    void returnsDeclinedResponseWithoutAuthorizationCodeAndUsesProvidedRrn() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        TpeRequestListener listener = new TpeRequestListener(service);
        ISOSource source = mock(ISOSource.class);
        TransactionMonetique declined = new TransactionMonetique();
        declined.setStatut(StatutTransaction.DECLINED);
        declined.setCodeReponse("54");
        when(service.autoriserTransactionTpe(eq("TPE-002"), any(BigDecimal.class), any(),
            eq("000002"), eq("987654321012"), eq(true), eq(true))).thenReturn(declined);

        ISOMsg request = new ISOMsg();
        request.setMTI("0200");
        request.set(2, "4111111111110000");
        request.set(4, "000000000100");
        request.set(11, "000002");
        request.set(37, "987654321012");
        request.set(41, "TPE-002");

        assertTrue(listener.process(source, request));
        ArgumentCaptor<ISOMsg> response = ArgumentCaptor.forClass(ISOMsg.class);
        verify(source).send(response.capture());
        assertEquals("54", response.getValue().getString(39));
        assertFalse(response.getValue().hasField(38));
        verify(service).autoriserTransactionTpe(eq("TPE-002"), argThat(amount -> amount.compareTo(BigDecimal.ONE) == 0),
            any(), eq("000002"), eq("987654321012"), eq(true), eq(true));
    }

    @Test
    void marksShortPanAsInvalidAndNotExpired() throws Exception {
        AuthorizationService service = mock(AuthorizationService.class);
        TpeRequestListener listener = new TpeRequestListener(service);
        ISOSource source = mock(ISOSource.class);
        TransactionMonetique declined = new TransactionMonetique();
        declined.setStatut(StatutTransaction.DECLINED);
        declined.setCodeReponse("14");
        when(service.autoriserTransactionTpe(eq("TPE-003"), any(), any(), eq("000003"),
            eq("000003"), eq(false), eq(false))).thenReturn(declined);

        ISOMsg request = new ISOMsg();
        request.setMTI("0200");
        request.set(2, "1234");
        request.set(4, "000000000100");
        request.set(11, "000003");
        request.set(41, "TPE-003");

        assertTrue(listener.process(source, request));
        verify(service).autoriserTransactionTpe(eq("TPE-003"), any(), any(), eq("000003"),
            eq("000003"), eq(false), eq(false));
    }
}
