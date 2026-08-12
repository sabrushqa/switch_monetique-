package com.lanacash.switchmonetique.controllers;

import jakarta.validation.constraints.NotBlank;

public class TpeAssignRequest {

    @NotBlank
    private String idCommercant;

    @NotBlank
    private String idPdv;

    /** Optionnels : permettent de creer/mettre a jour la fiche Commercant cote
     * switch monetique en meme temps que l'affectation. Si absents, seule
     * l'affectation du TPE est effectuee (comportement inchange). */
    private String nomCommercial;

    private String typeAffiliation;

    private String region;

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
}
