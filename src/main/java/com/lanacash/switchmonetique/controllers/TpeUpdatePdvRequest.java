package com.lanacash.switchmonetique.controllers;

import jakarta.validation.constraints.NotBlank;

public class TpeUpdatePdvRequest {

    @NotBlank
    private String idPdv;

    public String getIdPdv() {
        return idPdv;
    }

    public void setIdPdv(String idPdv) {
        this.idPdv = idPdv;
    }
}
