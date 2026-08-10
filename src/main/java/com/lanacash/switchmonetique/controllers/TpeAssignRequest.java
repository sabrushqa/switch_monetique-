package com.lanacash.switchmonetique.controllers;

import jakarta.validation.constraints.NotBlank;

public class TpeAssignRequest {

    @NotBlank
    private String idCommercant;

    @NotBlank
    private String idPdv;

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
}
