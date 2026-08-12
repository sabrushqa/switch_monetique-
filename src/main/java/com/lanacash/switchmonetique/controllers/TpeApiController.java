package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.Commercant;
import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.repositories.CommercantRepository;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
    private final CommercantRepository commercantRepository;

    public TpeApiController(TpeRepository tpeRepository, CommercantRepository commercantRepository) {
        this.tpeRepository = tpeRepository;
        this.commercantRepository = commercantRepository;
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

    @Transactional
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
                    upsertCommercant(request);
                    tpe.setIdCommercant(request.getIdCommercant());
                    tpe.setIdPdv(request.getIdPdv());
                    return ResponseEntity.ok(tpeRepository.save(tpe));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Permet au commercant (via demo) de re-affecter un TPE deja recu a un
     * AUTRE point de vente parmi les siens — contrairement a /assign, cette
     * route ne touche pas id_commercant (deja fixe) ni la fiche Commercant,
     * seulement id_pdv. Le TPE doit deja etre affecte (id_commercant non nul) ;
     * demo est responsable de verifier que le PDV cible appartient bien au
     * commercant proprietaire du TPE avant d'appeler cette route.
     */
    @Transactional
    @PutMapping("/{id}/pdv")
    public ResponseEntity<Tpe> mettreAJourPdv(@PathVariable String id, @Valid @RequestBody TpeUpdatePdvRequest request) {
        return tpeRepository.findById(id)
                .map(tpe -> {
                    if (tpe.getIdCommercant() == null) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).<Tpe>build();
                    }
                    tpe.setIdPdv(request.getIdPdv());
                    return ResponseEntity.ok(tpeRepository.save(tpe));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Cree ou met a jour la fiche Commercant cote switch monetique au moment
     * de l'affectation du TPE. demo (SQL Server) reste la source de verite ;
     * cette copie ne sert qu'a satisfaire la reference id_commercant deja
     * presente sur TPE, sans jamais la contredire (mise a jour, pas de
     * suppression, actif toujours remis a true ici puisqu'on affecte).
     * Si nomCommercial n'est pas fourni (compat. anciens appelants), on ne
     * touche pas a la fiche Commercant — seule l'affectation TPE a lieu.
     */
    private void upsertCommercant(TpeAssignRequest request) {
        if (!StringUtils.hasText(request.getNomCommercial())) {
            return;
        }
        Commercant commercant = commercantRepository.findById(request.getIdCommercant())
                .orElseGet(Commercant::new);
        commercant.setIdCommercant(request.getIdCommercant());
        commercant.setNomCommercial(request.getNomCommercial());
        commercant.setTypeAffiliation(request.getTypeAffiliation());
        commercant.setRegion(request.getRegion());
        commercant.setActif(true);
        if (commercant.getDateCreation() == null) {
            commercant.setDateCreation(LocalDateTime.now());
        }
        commercantRepository.save(commercant);
    }
}
