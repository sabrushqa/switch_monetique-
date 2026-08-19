package com.lanacash.switchmonetique.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lanacash.switchmonetique.entities.SiteEcommerce;
import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import com.lanacash.switchmonetique.repositories.TransactionRepository;
import com.lanacash.switchmonetique.services.AuthorizationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class EcommerceAndTransactionControllerTest {

    @Test
    void provisionsReadsAndDeactivatesEcommerceSite() {
        SiteEcommerceRepository repository = mock(SiteEcommerceRepository.class);
        EcommerceSiteApiController controller = new EcommerceSiteApiController(repository);
        when(repository.findById("VAD-42")).thenReturn(Optional.empty());
        when(repository.save(any(SiteEcommerce.class))).thenAnswer(invocation -> invocation.getArgument(0));
        EcommerceSiteProvisionRequest request = new EcommerceSiteProvisionRequest();
        request.setIdCommercant("42");
        request.setUrl("https://shop.example");

        SiteEcommerce created = controller.provisionner("VAD-42", request);
        assertEquals("42", created.getIdCommercant());
        assertTrue(created.isActif());

        created.setActif(false);
        when(repository.findById("VAD-42")).thenReturn(Optional.of(created));
        SiteEcommerce reprovisioned = controller.provisionner("VAD-42", request);
        assertTrue(reprovisioned.isActif());

        when(repository.findById("VAD-42")).thenReturn(Optional.of(created));
        assertEquals(HttpStatus.OK, controller.parId("VAD-42").getStatusCode());
        assertFalse(controller.desactiver("VAD-42").getBody().isActif());

        when(repository.findById("MISSING")).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, controller.parId("MISSING").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.desactiver("MISSING").getStatusCode());
    }

    @Test
    void generatesNextIdBasedOnHighestExistingSuffix() {
        SiteEcommerceRepository repository = mock(SiteEcommerceRepository.class);
        EcommerceSiteApiController controller = new EcommerceSiteApiController(repository);
        when(repository.save(any(SiteEcommerce.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SiteEcommerce existingLow = new SiteEcommerce();
        existingLow.setIdSiteEcommerce("ECOM-000003");
        SiteEcommerce existingHigh = new SiteEcommerce();
        existingHigh.setIdSiteEcommerce("ECOM-000041");
        SiteEcommerce nonMatching = new SiteEcommerce();
        nonMatching.setIdSiteEcommerce("VAD-42");
        when(repository.findAll()).thenReturn(List.of(existingLow, existingHigh, nonMatching));

        EcommerceSiteProvisionRequest request = new EcommerceSiteProvisionRequest();
        request.setIdCommercant("42");
        request.setUrl("https://shop.example");

        SiteEcommerce created = controller.provisionnerNouveau(request);

        assertEquals("ECOM-000042", created.getIdSiteEcommerce());
        assertEquals("42", created.getIdCommercant());
        assertEquals("https://shop.example", created.getUrl());
        assertTrue(created.isActif());
    }

    @Test
    void generatesFirstIdWhenNoneExist() {
        SiteEcommerceRepository repository = mock(SiteEcommerceRepository.class);
        EcommerceSiteApiController controller = new EcommerceSiteApiController(repository);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any(SiteEcommerce.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EcommerceSiteProvisionRequest request = new EcommerceSiteProvisionRequest();
        request.setIdCommercant("7");
        request.setUrl("https://boutique.example");

        SiteEcommerce created = controller.provisionnerNouveau(request);

        assertEquals("ECOM-000001", created.getIdSiteEcommerce());
    }

    @Test
    void delegatesEcommercePaymentToAuthorizationEngine() {
        AuthorizationService service = mock(AuthorizationService.class);
        EcommerceTransactionController controller = new EcommerceTransactionController(service);
        EcommerceTransactionRequest request = new EcommerceTransactionRequest();
        request.setIdSiteEcommerce("VAD-42");
        request.setIdCommercant("42");
        request.setMontant(new BigDecimal("50"));
        request.setTypeTransaction(TypeTransaction.ACHAT);
        request.setAuthentification3dsReussie(true);
        TransactionMonetique expected = new TransactionMonetique();
        when(service.autoriserTransactionEcommerce(anyString(), anyString(), any(), any(), eq(true)))
            .thenReturn(expected);

        assertSame(expected, controller.creerTransaction(request).getBody());
    }

    @Test
    void exposesTransactionsByMerchantAndId() {
        TransactionRepository repository = mock(TransactionRepository.class);
        TransactionApiController controller = new TransactionApiController(repository);
        TransactionMonetique transaction = new TransactionMonetique();
        transaction.setIdTransaction("tx-1");
        when(repository.findByIdCommercantOrderByDateTransactionDesc("42")).thenReturn(List.of(transaction));
        when(repository.findById("tx-1")).thenReturn(Optional.of(transaction));
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertEquals(1, controller.parCommercant("42").size());
        assertEquals(HttpStatus.OK, controller.parId("tx-1").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.parId("missing").getStatusCode());
    }
}
