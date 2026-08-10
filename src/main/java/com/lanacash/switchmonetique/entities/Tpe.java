package com.lanacash.switchmonetique.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tpe")
public class Tpe {

    @Id
    @Column(name = "id_tpe", length = 20)
    private String idTpe;

    /** Null tant que le TPE est en stock et n'a pas ete affecte a un commercant. */
    @Column(name = "id_commercant", length = 50)
    private String idCommercant;

    /** Null tant que le TPE est en stock et n'a pas ete affecte a un point de vente. */
    @Column(name = "id_pdv", length = 50)
    private String idPdv;

    /** TPE / SOFTPOS / QR_CODE. */
    @Column(name = "nature", length = 20)
    private String nature;

    /** ETHERNET / WIFI / GPRS_3G_4G / NFC_TELEPHONE / BLUETOOTH / AFFICHAGE. */
    @Column(name = "connectivite", length = 20)
    private String connectivite;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "plafond_journalier", nullable = false)
    private BigDecimal plafondJournalier = BigDecimal.valueOf(50000);

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public String getIdTpe() {
        return idTpe;
    }

    public void setIdTpe(String idTpe) {
        this.idTpe = idTpe;
    }

    public String getIdCommercant() {
        return idCommercant;
    }

    public void setIdCommercant(String idCommercant) {
        this.idCommercant = idCommercant;
    }

    public String getIdPdv() {
        return idPdv;
    }

    public void setIdPdv(String idPdv) {
        this.idPdv = idPdv;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getConnectivite() {
        return connectivite;
    }

    public void setConnectivite(String connectivite) {
        this.connectivite = connectivite;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public BigDecimal getPlafondJournalier() {
        return plafondJournalier;
    }

    public void setPlafondJournalier(BigDecimal plafondJournalier) {
        this.plafondJournalier = plafondJournalier;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
