package com.lanacash.switchmonetique.entities.enums;

/** Codes reponse inspires de l'ISO 8583 (DE39). */
public enum CodeReponse {
    APPROUVE("00"),
    REFUS_GENERAL("05"),
    CARTE_INVALIDE("14"),
    SOLDE_INSUFFISANT("51"),
    CARTE_EXPIREE("54"),
    SUSPICION_FRAUDE("59"),
    PLAFOND_DEPASSE("61"),
    ECHEC_AUTHENTIFICATION_3DS("65"),
    TRANSACTION_DUPLIQUEE("94"),
    PANNE_SYSTEME("96");

    private final String code;

    CodeReponse(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
