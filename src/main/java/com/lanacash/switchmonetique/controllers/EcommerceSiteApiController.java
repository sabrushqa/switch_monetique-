package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.SiteEcommerce;
import com.lanacash.switchmonetique.repositories.SiteEcommerceRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
