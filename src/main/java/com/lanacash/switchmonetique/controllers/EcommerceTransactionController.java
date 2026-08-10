package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.services.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Canal d'entree pour les sites/applications e-commerce (equivalent d'une passerelle
 * de paiement REST, a la difference du canal TPE qui parle ISO 8583 en TCP).
 */
@RestController
@RequestMapping("/api/ecommerce/transactions")
public class EcommerceTransactionController {

    private final AuthorizationService authorizationService;

    public EcommerceTransactionController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<TransactionMonetique> creerTransaction(@Valid @RequestBody EcommerceTransactionRequest request) {
        TransactionMonetique transaction = authorizationService.autoriserTransactionEcommerce(
                request.getIdSiteEcommerce(),
                request.getIdCommercant(),
                request.getMontant(),
                request.getTypeTransaction(),
                request.isAuthentification3dsReussie()
        );
        return ResponseEntity.ok(transaction);
    }
}
