package com.lanacash.switchmonetique.services;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.entities.enums.CanalTransaction;
import com.lanacash.switchmonetique.entities.enums.CodeReponse;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import com.lanacash.switchmonetique.repositories.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Moteur d'autorisation commun aux deux canaux (TPE / e-commerce).
 * Reproduit les regles de decision d'un switch monetique reel : affiliation, plafond, fraude/velocite.
 */
@Service
public class AuthorizationService {

    private static final int MAX_TRANSACTIONS_PAR_MINUTE = 5;

    private final TransactionRepository transactionRepository;
    private final TpeRepository tpeRepository;
    private final DemoTransactionNotifier demoTransactionNotifier;
    private final SiteEcommerceRepository siteEcommerceRepository;

    public AuthorizationService(TransactionRepository transactionRepository, TpeRepository tpeRepository,
                                DemoTransactionNotifier demoTransactionNotifier,
                                SiteEcommerceRepository siteEcommerceRepository) {
        this.transactionRepository = transactionRepository;
        this.tpeRepository = tpeRepository;
        this.demoTransactionNotifier = demoTransactionNotifier;
        this.siteEcommerceRepository = siteEcommerceRepository;
    }

    public TransactionMonetique autoriserTransactionTpe(String idTpe, BigDecimal montant, TypeTransaction type,
                                                         String stan, String rrn, boolean carteValide,
                                                         boolean carteExpiree) {
        TransactionMonetique transaction = nouvelleTransaction(CanalTransaction.TPE, montant, type);
        transaction.setIdTpe(idTpe);
        transaction.setStan(stan);
        transaction.setRrn(rrn);

        Optional<Tpe> tpeOpt = tpeRepository.findById(idTpe);
        if (tpeOpt.isEmpty() || !tpeOpt.get().isActif()
                || tpeOpt.get().getIdCommercant() == null || tpeOpt.get().getIdPdv() == null) {
            return refuser(transaction, CodeReponse.REFUS_GENERAL, "TPE non affilie ou desactive");
        }
        Tpe tpe = tpeOpt.get();
        transaction.setIdCommercant(tpe.getIdCommercant());

        if (!carteValide) {
            return refuser(transaction, CodeReponse.CARTE_INVALIDE, "Numero de carte invalide");
        }
        if (carteExpiree) {
            return refuser(transaction, CodeReponse.CARTE_EXPIREE, "Carte expiree");
        }

        LocalDateTime debutJour = LocalDateTime.of(LocalDateTime.now().toLocalDate(), LocalTime.MIDNIGHT);
        BigDecimal cumulJour = transactionRepository.sumMontantApprouveDepuis(idTpe, debutJour);
        if (cumulJour.add(montant).compareTo(tpe.getPlafondJournalier()) > 0) {
            return refuser(transaction, CodeReponse.PLAFOND_DEPASSE, "Plafond journalier depasse");
        }

        long transactionsRecentes = transactionRepository.countByIdTpeAndDateTransactionAfter(
                idTpe, LocalDateTime.now().minusMinutes(1));
        if (transactionsRecentes >= MAX_TRANSACTIONS_PAR_MINUTE) {
            return refuser(transaction, CodeReponse.SUSPICION_FRAUDE, "Velocite de transactions anormale");
        }

        return approuver(transaction);
    }

    public TransactionMonetique autoriserTransactionEcommerce(String idSiteEcommerce, String idCommercant,
                                                                BigDecimal montant, TypeTransaction type,
                                                                boolean authentification3dsReussie) {
        TransactionMonetique transaction = nouvelleTransaction(CanalTransaction.ECOMMERCE, montant, type);
        transaction.setIdSiteEcommerce(idSiteEcommerce);
        transaction.setIdCommercant(idCommercant);

        var site = siteEcommerceRepository.findById(idSiteEcommerce).orElse(null);
        if (site == null || !site.isActif() || !idCommercant.equals(site.getIdCommercant())) {
            return refuser(transaction, CodeReponse.REFUS_GENERAL, "Site e-commerce non affilié ou désactivé");
        }

        if (!authentification3dsReussie) {
            return refuser(transaction, CodeReponse.ECHEC_AUTHENTIFICATION_3DS, "Authentification 3D Secure echouee");
        }

        return approuver(transaction);
    }

    private TransactionMonetique nouvelleTransaction(CanalTransaction canal, BigDecimal montant, TypeTransaction type) {
        TransactionMonetique transaction = new TransactionMonetique();
        transaction.setIdTransaction(UUID.randomUUID().toString());
        transaction.setCanal(canal);
        transaction.setMontant(montant);
        transaction.setTypeTransaction(type);
        transaction.setDateTransaction(LocalDateTime.now());
        return transaction;
    }

    private TransactionMonetique approuver(TransactionMonetique transaction) {
        transaction.setStatut(StatutTransaction.APPROVED);
        transaction.setCodeReponse(CodeReponse.APPROUVE.getCode());
        transaction.setCodeAutorisation(genererCodeAutorisation());
        TransactionMonetique saved = transactionRepository.save(transaction);
        demoTransactionNotifier.notifyDemo(saved);
        return saved;
    }

    private TransactionMonetique refuser(TransactionMonetique transaction, CodeReponse code, String message) {
        transaction.setStatut(StatutTransaction.DECLINED);
        transaction.setCodeReponse(code.getCode());
        transaction.setMessageErreur(message);
        TransactionMonetique saved = transactionRepository.save(transaction);
        demoTransactionNotifier.notifyDemo(saved);
        return saved;
    }

    private String genererCodeAutorisation() {
        return String.valueOf((int) (100000 + Math.random() * 899999));
    }
}
