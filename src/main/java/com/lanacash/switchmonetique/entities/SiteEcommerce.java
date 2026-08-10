package com.lanacash.switchmonetique.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Site/application e-commerce affilie (canal REST/JSON). Contrairement au TPE, ce n'est
 * pas un terminal physique : c'est l'identifiant du site marchand qui appelle l'API du switch.
 */
@Entity
@Table(name = "site_ecommerce")
public class SiteEcommerce {

    @Id
    @Column(name = "id_site_ecommerce", length = 50)
    private String idSiteEcommerce;

    @Column(name = "id_commercant", length = 50)
    private String idCommercant;

    @Column(name = "url", length = 255)
    private String url;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
