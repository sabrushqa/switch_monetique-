package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.SiteEcommerce;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de provisionnement du terminal virtuel/VAD appelée par demo. */
@RestController
@RequestMapping("/api/switch/ecommerce-sites")
public class EcommerceSiteApiController {

    private final SiteEcommerceRepository repository;

    public EcommerceSiteApiController(SiteEcommerceRepository repository) {
        this.repository = repository;
    }

    /**
     * Provisionne un NOUVEAU site e-commerce avec un identifiant genere ici,
     * cote switch — contrairement au TPE (stock physique pre-existant que
     * l'appelant choisit), un site e-commerce n'a pas d'inventaire : c'est
     * switch-monetique-service, source de verite du canal, qui doit fournir
     * l'identifiant plutot que de laisser le BOA en inventer un a la main
     * (risque de collision/erreur de saisie, et incoherence avec le modele
     * TPE ou l'ID vient toujours de ce service).
     */
    @PostMapping
    public SiteEcommerce provisionnerNouveau(@Valid @RequestBody EcommerceSiteProvisionRequest request) {
        SiteEcommerce site = new SiteEcommerce();
        site.setIdSiteEcommerce(genererProchainId());
        site.setIdCommercant(request.getIdCommercant());
        site.setUrl(request.getUrl());
        site.setActif(true);
        site.setDateCreation(LocalDateTime.now());
        return repository.save(site);
    }

    /**
     * ECOM-000001, ECOM-000002, ... a partir du plus grand suffixe numerique
     * deja attribue. Volume attendu faible (un site par dossier e-commerce) —
     * un simple scan de la table suffit pour ce simulateur, pas besoin d'une
     * sequence SQL dediee.
     */
    private String genererProchainId() {
        int next = repository.findAll().stream()
                .map(SiteEcommerce::getIdSiteEcommerce)
                .filter(id -> id != null && id.startsWith("ECOM-") && id.substring(5).chars().allMatch(Character::isDigit))
                .mapToInt(id -> Integer.parseInt(id.substring(5)))
                .max()
                .orElse(0) + 1;
        return "ECOM-" + String.format("%06d", next);
    }

    /**
     * @deprecated Conserve pour compatibilite (provisionnement idempotent
     * avec un ID impose) mais demo utilise desormais provisionnerNouveau()
     * qui laisse switch generer l'identifiant.
     */
    @Deprecated
    @PutMapping("/{id}")
    public SiteEcommerce provisionner(
            @PathVariable String id,
            @Valid @RequestBody EcommerceSiteProvisionRequest request
    ) {
        SiteEcommerce site = repository.findById(id).orElseGet(SiteEcommerce::new);
        if (site.getIdSiteEcommerce() == null) {
            site.setIdSiteEcommerce(id);
            site.setDateCreation(LocalDateTime.now());
        }
        site.setIdCommercant(request.getIdCommercant());
        site.setUrl(request.getUrl());
        site.setActif(true);
        return repository.save(site);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteEcommerce> parId(@PathVariable String id) {
        return repository.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<SiteEcommerce> desactiver(@PathVariable String id) {
        return repository.findById(id).map(site -> {
            site.setActif(false);
            return ResponseEntity.ok(repository.save(site));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
