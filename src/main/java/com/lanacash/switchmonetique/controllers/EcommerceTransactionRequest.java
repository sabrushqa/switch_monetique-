package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.enums.TypeTransaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class EcommerceTransactionRequest {

    @NotBlank
    private String idSiteEcommerce;

    @NotBlank
    private String idCommercant;

    @NotNull
    @Positive
    private BigDecimal montant;

    @NotNull
    private TypeTransaction typeTransaction;

    /** Resultat de l'authentification 3D Secure cote site e-commerce. */
    private boolean authentification3dsReussie;

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

    public TypeTransaction getTypeTransaction() {
        return typeTransaction;
    }

    public void setTypeTransaction(TypeTransaction typeTransaction) {
        this.typeTransaction = typeTransaction;
    }

    public boolean isAuthentification3dsReussie() {
        return authentification3dsReussie;
    }

    public void setAuthentification3dsReussie(boolean authentification3dsReussie) {
        this.authentification3dsReussie = authentification3dsReussie;
    }
}
