package com.lanacash.switchmonetique.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.lanacash.switchmonetique.entities.SiteEcommerce;
import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import com.lanacash.switchmonetique.repositories.TransactionRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {
    @Mock TransactionRepository transactionRepository;
    @Mock TpeRepository tpeRepository;
    @Mock SiteEcommerceRepository siteRepository;
    @Mock DemoTransactionNotifier notifier;
    private AuthorizationService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationService(transactionRepository, tpeRepository, notifier, siteRepository);
        when(transactionRepository.save(any(TransactionMonetique.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvesTransactionFromActiveAssignedTpe() {
        Tpe tpe = assignedTpe("TPE-001", "42", "8", "50000");
        when(tpeRepository.findById("TPE-001")).thenReturn(Optional.of(tpe));
        when(transactionRepository.sumMontantApprouveDepuis(anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByIdTpeAndDateTransactionAfter(anyString(), any())).thenReturn(0L);

        TransactionMonetique result = service.autoriserTransactionTpe(
            "TPE-001", new BigDecimal("125.50"), TypeTransaction.ACHAT, "4111111111111111",
            "000001", "123456789012", true, false);

        assertEquals(StatutTransaction.APPROVED, result.getStatut());
        assertEquals("00", result.getCodeReponse());
        assertEquals("42", result.getIdCommercant());
        assertEquals("XXXXXXXXXXXX1111", result.getPanMasque());
        assertEquals("VISA", result.getTypeCarte());
        assertEquals("A0000000031010", result.getAid());
        verify(notifier).notifyDemo(result);
    }

    @Test
    void refusesUnknownOrUnassignedTpe() {
        when(tpeRepository.findById("TPE-002"))
            .thenReturn(Optional.of(assignedTpe("TPE-002", null, null, "50000")));

        TransactionMonetique result = service.autoriserTransactionTpe(
            "TPE-002", BigDecimal.TEN, TypeTransaction.ACHAT, "4111111111111111",
            "000002", "123456789013", true, false);

        assertEquals(StatutTransaction.DECLINED, result.getStatut());
        assertEquals("05", result.getCodeReponse());
        assertNull(result.getIdCommercant());
    }

    @Test
    void refusesExpiredCardAndExceededDailyLimit() {
        when(tpeRepository.findById("TPE-003"))
            .thenReturn(Optional.of(assignedTpe("TPE-003", "42", "8", "100")));

        TransactionMonetique expired = service.autoriserTransactionTpe(
            "TPE-003", BigDecimal.TEN, TypeTransaction.ACHAT, "5111111111111111",
            "000003", "123456789014", true, true);
        assertEquals("54", expired.getCodeReponse());

        when(transactionRepository.sumMontantApprouveDepuis(anyString(), any())).thenReturn(new BigDecimal("90"));
        TransactionMonetique overLimit = service.autoriserTransactionTpe(
            "TPE-003", new BigDecimal("20"), TypeTransaction.ACHAT, "5111111111111111",
            "000004", "123456789015", true, false);
        assertEquals("61", overLimit.getCodeReponse());
    }

    @Test
    void refusesInvalidCardAndSuspiciousVelocity() {
        when(tpeRepository.findById("TPE-004"))
            .thenReturn(Optional.of(assignedTpe("TPE-004", "42", "8", "50000")));

        TransactionMonetique invalidCard = service.autoriserTransactionTpe(
            "TPE-004", BigDecimal.TEN, TypeTransaction.ACHAT, "000", "000005", "123456789016", false, false);
        assertEquals("14", invalidCard.getCodeReponse());

        when(transactionRepository.sumMontantApprouveDepuis(anyString(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countByIdTpeAndDateTransactionAfter(anyString(), any())).thenReturn(5L);
        TransactionMonetique suspicious = service.autoriserTransactionTpe(
            "TPE-004", BigDecimal.TEN, TypeTransaction.ACHAT, "4111111111111111",
            "000006", "123456789017", true, false);
        assertEquals("59", suspicious.getCodeReponse());
    }

    @Test
    void refusesInactiveAndMissingTerminals() {
        Tpe inactive = assignedTpe("TPE-005", "42", "8", "50000");
        inactive.setActif(false);
        when(tpeRepository.findById("TPE-005")).thenReturn(Optional.of(inactive));
        when(tpeRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertEquals("05", service.autoriserTransactionTpe(
            "TPE-005", BigDecimal.ONE, TypeTransaction.ACHAT, "4111111111111111",
            "000007", "123456789018", true, false
        ).getCodeReponse());
        assertEquals("05", service.autoriserTransactionTpe(
            "MISSING", BigDecimal.ONE, TypeTransaction.ACHAT, "4111111111111111",
            "000008", "123456789019", true, false
        ).getCodeReponse());
    }

    @Test
    void refusesTpeAssignedToCommercantButWithoutPdv() {
        // Cas distinct de refusesUnknownOrUnassignedTpe (qui met idCommercant ET
        // idPdv a null) : ici seul idPdv est manquant, pour forcer l'evaluation
        // du second operande du OU (idCommercant == null || idPdv == null).
        Tpe missingPdv = assignedTpe("TPE-006", "42", null, "50000");
        when(tpeRepository.findById("TPE-006")).thenReturn(Optional.of(missingPdv));

        assertEquals("05", service.autoriserTransactionTpe(
            "TPE-006", BigDecimal.ONE, TypeTransaction.ACHAT, "4111111111111111",
            "000009", "123456789020", true, false
        ).getCodeReponse());
    }

    @Test
    void computesPanMaskingAndCardTypeForEdgeCasePans() {
        // Ces cas passent par un TPE inconnu : maskPan/detectTypeCarte sont
        // calcules avant la verification du TPE, donc pas besoin de mocker un
        // Tpe valide pour couvrir ces branches purement liees au PAN.
        when(tpeRepository.findById(anyString())).thenReturn(Optional.empty());

        TransactionMonetique nullPan = service.autoriserTransactionTpe(
            "X", BigDecimal.ONE, TypeTransaction.ACHAT, null, "1", "1", false, false);
        assertNull(nullPan.getPanMasque());
        assertEquals("CARTE LOCALE", nullPan.getTypeCarte());

        TransactionMonetique emptyPan = service.autoriserTransactionTpe(
            "X", BigDecimal.ONE, TypeTransaction.ACHAT, "", "1", "1", false, false);
        assertEquals("CARTE LOCALE", emptyPan.getTypeCarte());

        TransactionMonetique tooShortPan = service.autoriserTransactionTpe(
            "X", BigDecimal.ONE, TypeTransaction.ACHAT, "0", "1", "1", false, false);
        assertEquals("CARTE LOCALE", tooShortPan.getTypeCarte());

        TransactionMonetique mastercardParPrefixe2 = service.autoriserTransactionTpe(
            "X", BigDecimal.ONE, TypeTransaction.ACHAT, "5500000000000000", "1", "1", false, false);
        assertEquals("MASTERCARD", mastercardParPrefixe2.getTypeCarte());
        assertEquals("A0000000041010", mastercardParPrefixe2.getAid());

        TransactionMonetique mastercardParPrefixe4 = service.autoriserTransactionTpe(
            "X", BigDecimal.ONE, TypeTransaction.ACHAT, "2222000000000000", "1", "1", false, false);
        assertEquals("MASTERCARD", mastercardParPrefixe4.getTypeCarte());
    }

    @Test
    void validatesEcommerceAffiliationAnd3ds() {
        SiteEcommerce site = new SiteEcommerce();
        site.setIdSiteEcommerce("VAD-42");
        site.setIdCommercant("42");
        site.setActif(true);
        when(siteRepository.findById("VAD-42")).thenReturn(Optional.of(site));

        TransactionMonetique approved = service.autoriserTransactionEcommerce(
            "VAD-42", "42", new BigDecimal("99.90"), TypeTransaction.ACHAT, true);
        assertEquals("00", approved.getCodeReponse());

        TransactionMonetique failed3ds = service.autoriserTransactionEcommerce(
            "VAD-42", "42", new BigDecimal("99.90"), TypeTransaction.ACHAT, false);
        assertEquals("65", failed3ds.getCodeReponse());

        TransactionMonetique wrongMerchant = service.autoriserTransactionEcommerce(
            "VAD-42", "99", new BigDecimal("99.90"), TypeTransaction.ACHAT, true);
        assertEquals("05", wrongMerchant.getCodeReponse());

        when(siteRepository.findById("VAD-INCONNU")).thenReturn(Optional.empty());
        TransactionMonetique unknownSite = service.autoriserTransactionEcommerce(
            "VAD-INCONNU", "42", new BigDecimal("10.00"), TypeTransaction.ACHAT, true);
        assertEquals("05", unknownSite.getCodeReponse());

        SiteEcommerce inactiveSite = new SiteEcommerce();
        inactiveSite.setIdSiteEcommerce("VAD-99");
        inactiveSite.setIdCommercant("42");
        inactiveSite.setActif(false);
        when(siteRepository.findById("VAD-99")).thenReturn(Optional.of(inactiveSite));
        TransactionMonetique inactiveSiteResult = service.autoriserTransactionEcommerce(
            "VAD-99", "42", new BigDecimal("10.00"), TypeTransaction.ACHAT, true);
        assertEquals("05", inactiveSiteResult.getCodeReponse());
    }

    private Tpe assignedTpe(String id, String merchant, String pdv, String limit) {
        Tpe tpe = new Tpe();
        tpe.setIdTpe(id);
        tpe.setIdCommercant(merchant);
        tpe.setIdPdv(pdv);
        tpe.setActif(true);
        tpe.setPlafondJournalier(new BigDecimal(limit));
        return tpe;
    }
}
