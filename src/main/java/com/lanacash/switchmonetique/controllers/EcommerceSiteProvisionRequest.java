package com.lanacash.switchmonetique.controllers;

import jakarta.validation.constraints.NotBlank;

public class EcommerceSiteProvisionRequest {
    @NotBlank
    private String idCommercant;
    @NotBlank
    private String url;

    public String getIdCommercant() { return idCommercant; }
    public void setIdCommercant(String idCommercant) { this.idCommercant = idCommercant; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
