package com.lanacash.switchmonetique.entities;

import com.lanacash.switchmonetique.entities.enums.CanalTransaction;
import com.lanacash.switchmonetique.entities.enums.StatutTransaction;
import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_monetique")
public class TransactionMonetique {

    @Id
    @Column(name = "id_transaction", length = 36)
    private String idTransaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 15)
    private CanalTransaction canal;

    @Column(name = "id_tpe", length = 20)
    private String idTpe;

    @Column(name = "id_site_ecommerce", length = 50)
    private String idSiteEcommerce;

    // Peut être null sur une tentative refusée provenant d'un terminal inconnu/non affecté.
    @Column(name = "id_commercant", length = 50)
    private String idCommercant;

    @Column(name = "montant", nullable = false)
    private BigDecimal montant;

    @Column(name = "devise", nullable = false, length = 3)
    private String devise = "MAD";

    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false, length = 20)
    private TypeTransaction typeTransaction;

    @Column(name = "stan", length = 6)
    private String stan;

    @Column(name = "rrn", length = 12)
    private String rrn;

    @Column(name = "code_autorisation", length = 6)
    private String codeAutorisation;

    @Column(name = "code_reponse", length = 2)
    private String codeReponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 15)
    private StatutTransaction statut;

    @Column(name = "mode_paiement", length = 10)
    private String mode = "ONLINE";

    @Column(name = "message_erreur", length = 255)
    private String messageErreur;

    @Column(name = "date_transaction", nullable = false)
    private LocalDateTime dateTransaction = LocalDateTime.now();

    public String getIdTransaction() {
        return idTransaction;
    }

    public void setIdTransaction(String idTransaction) {
        this.idTransaction = idTransaction;
    }

    public CanalTransaction getCanal() {
        return canal;
    }

    public void setCanal(CanalTransaction canal) {
        this.canal = canal;
    }

    public String getIdTpe() {
        return idTpe;
    }

    public void setIdTpe(String idTpe) {
        this.idTpe = idTpe;
    }

    public String getIdSiteEcommerce() {
        return idSiteEcommerce;
    }

    public void setIdSiteEcommerce(String idSiteEcommerce) {
        this.idSiteEcommerce = idSiteEcommerce;
    }

    public String getIdCommercant() {
        return idCommercant;
    }

    public void setIdCommercant(String idCommercant) {
        this.idCommercant = idCommercant;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public TypeTransaction getTypeTransaction() {
        return typeTransaction;
    }

    public void setTypeTransaction(TypeTransaction typeTransaction) {
        this.typeTransaction = typeTransaction;
    }

    public String getStan() {
        return stan;
    }

    public void setStan(String stan) {
        this.stan = stan;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(String rrn) {
        this.rrn = rrn;
    }

    public String getCodeAutorisation() {
        return codeAutorisation;
    }

    public void setCodeAutorisation(String codeAutorisation) {
        this.codeAutorisation = codeAutorisation;
    }

    public String getCodeReponse() {
        return codeReponse;
    }

    public void setCodeReponse(String codeReponse) {
        this.codeReponse = codeReponse;
    }

    public StatutTransaction getStatut() {
        return statut;
    }

    public void setStatut(StatutTransaction statut) {
        this.statut = statut;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getMessageErreur() {
        return messageErreur;
    }

    public void setMessageErreur(String messageErreur) {
        this.messageErreur = messageErreur;
    }

    public LocalDateTime getDateTransaction() {
        return dateTransaction;
    }

    public void setDateTransaction(LocalDateTime dateTransaction) {
        this.dateTransaction = dateTransaction;
    }
}
