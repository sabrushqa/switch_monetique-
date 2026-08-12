package com.lanacash.switchmonetique.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Reflet minimal du commercant cote switch monetique : cree/mis a jour au
 * moment ou un TPE lui est affecte (voir TpeApiController.affecter). La
 * source de verite de l'identite complete du commercant reste le projet
 * "demo" (SQL Server) ; cette table ne sert qu'a satisfaire la reference
 * id_commercant deja presente sur TPE et a permettre des requetes cote
 * switch monetique sans repasser par demo.
 */
@Entity
@Table(name = "commercant")
public class Commercant {

    @Id
    @Column(name = "id_commercant", length = 50)
    private String idCommercant;

    @Column(name = "nom_commercial", nullable = false, length = 200)
    private String nomCommercial;

    @Column(name = "type_affiliation", length = 50)
    private String typeAffiliation;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public String getIdCommercant() {
        return idCommercant;
    }

    public void setIdCommercant(String idCommercant) {
        this.idCommercant = idCommercant;
    }

    public String getNomCommercial() {
        return nomCommercial;
    }

    public void setNomCommercial(String nomCommercial) {
        this.nomCommercial = nomCommercial;
    }

    public String getTypeAffiliation() {
        return typeAffiliation;
    }

    public void setTypeAffiliation(String typeAffiliation) {
        this.typeAffiliation = typeAffiliation;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
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
