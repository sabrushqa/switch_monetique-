package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API exposee au projet "demo" pour le stock de TPE : demo ne cree plus de TPE
 * localement, il consulte ce stock (GET) et fait affecter un TPE a un commercant
 * (PUT .../assign) au moment ou le superviseur/BOA valide l'affectation cote portail.
 */
@RestController
@RequestMapping("/api/switch/tpes")
public class TpeApiController {

    private final TpeRepository tpeRepository;

    public TpeApiController(TpeRepository tpeRepository) {
        this.tpeRepository = tpeRepository;
    }

    @GetMapping
    public List<Tpe> stock(
            @RequestParam(required = false) String nature,
            @RequestParam(defaultValue = "false") boolean disponible
    ) {
        List<Tpe> tpes = disponible
                ? (nature == null || nature.isBlank()
                    ? tpeRepository.findByIdCommercantIsNullAndActifTrue()
                    : tpeRepository.findByIdCommercantIsNullAndActifTrueAndNature(nature.toUpperCase()))
                : tpeRepository.findAll();

        if (!disponible && nature != null && !nature.isBlank()) {
            String natureFilter = nature.toUpperCase();
            tpes = tpes.stream().filter(tpe -> natureFilter.equals(tpe.getNature())).toList();
        }
        return tpes;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tpe> parId(@PathVariable String id) {
        return tpeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Tpe> activer(@PathVariable String id) {
        return tpeRepository.findById(id)
                .map(tpe -> {
                    tpe.setActif(true);
                    return ResponseEntity.ok(tpeRepository.save(tpe));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Tpe> desactiver(@PathVariable String id) {
        return tpeRepository.findById(id)
                .map(tpe -> {
                    tpe.setActif(false);
                    return ResponseEntity.ok(tpeRepository.save(tpe));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<Tpe> affecter(@PathVariable String id, @Valid @RequestBody TpeAssignRequest request) {
        return tpeRepository.findById(id)
                .map(tpe -> {
                    if (!tpe.isActif()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Tpe>build();
                    }
                    if (tpe.getIdCommercant() != null) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Tpe>build();
                    }
                    tpe.setIdCommercant(request.getIdCommercant());
                    tpe.setIdPdv(request.getIdPdv());
                    return ResponseEntity.ok(tpeRepository.save(tpe));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
