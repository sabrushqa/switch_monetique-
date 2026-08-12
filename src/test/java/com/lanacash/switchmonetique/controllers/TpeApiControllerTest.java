package com.lanacash.switchmonetique.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.lanacash.switchmonetique.entities.Commercant;
import com.lanacash.switchmonetique.entities.Tpe;
import com.lanacash.switchmonetique.repositories.CommercantRepository;
import com.lanacash.switchmonetique.repositories.TpeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

class TpeApiControllerTest {
    private final TpeRepository repository = mock(TpeRepository.class);
    private final CommercantRepository commercantRepository = mock(CommercantRepository.class);
    private final TpeApiController controller = new TpeApiController(repository, commercantRepository);

    @Test
    void listsCompleteAndAvailableStockWithNatureFilter() {
        Tpe tpe = terminal("TPE-1", true, null);
        Tpe softpos = terminal("SOFT-1", true, null);
        softpos.setNature("SOFTPOS");
        when(repository.findAll()).thenReturn(List.of(tpe));
        when(repository.findByIdCommercantIsNullAndActifTrue()).thenReturn(List.of(tpe, softpos));
        when(repository.findByIdCommercantIsNullAndActifTrueAndNature("TPE")).thenReturn(List.of(tpe));

        assertEquals(1, controller.stock(null, false).size());
        assertEquals(1, controller.stock("", false).size());
        assertEquals(1, controller.stock("tpe", true).size());
        assertEquals(2, controller.stock("", true).size());

        when(repository.findAll()).thenReturn(List.of(tpe, softpos));
        assertEquals(List.of(tpe), controller.stock("TPE", false));
    }

    @Test
    void activatesAndDeactivatesExistingTerminalAndReturns404Otherwise() {
        Tpe tpe = terminal("TPE-1", false, null);
        when(repository.findById("TPE-1")).thenReturn(Optional.of(tpe));
        when(repository.findById("missing")).thenReturn(Optional.empty());
        when(repository.save(tpe)).thenReturn(tpe);

        assertTrue(controller.activer("TPE-1").getBody().isActif());
        assertFalse(controller.desactiver("TPE-1").getBody().isActif());
        assertEquals(HttpStatus.OK, controller.parId("TPE-1").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.parId("missing").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.activer("missing").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.desactiver("missing").getStatusCode());
    }

    @Test
    void assignsOnlyActiveAvailableTerminal() {
        Tpe available = terminal("TPE-1", true, null);
        when(repository.findById("TPE-1")).thenReturn(Optional.of(available));
        when(repository.save(available)).thenReturn(available);
        TpeAssignRequest request = new TpeAssignRequest();
        request.setIdCommercant("42");
        request.setIdPdv("8");

        var response = controller.affecter("TPE-1", request);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("42", response.getBody().getIdCommercant());
        assertEquals("8", response.getBody().getIdPdv());

        Tpe inactive = terminal("TPE-2", false, null);
        when(repository.findById("TPE-2")).thenReturn(Optional.of(inactive));
        assertEquals(HttpStatus.CONFLICT, controller.affecter("TPE-2", request).getStatusCode());

        Tpe occupied = terminal("TPE-3", true, "99");
        when(repository.findById("TPE-3")).thenReturn(Optional.of(occupied));
        assertEquals(HttpStatus.CONFLICT, controller.affecter("TPE-3", request).getStatusCode());

        verifyNoInteractions(commercantRepository);
    }

    @Test
    void assigningWithMerchantDetailsCreatesCommercantRecord() {
        Tpe available = terminal("TPE-CREATE", true, null);
        when(repository.findById("TPE-CREATE")).thenReturn(Optional.of(available));
        when(repository.save(available)).thenReturn(available);
        when(commercantRepository.findById("42")).thenReturn(Optional.empty());
        when(commercantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TpeAssignRequest request = new TpeAssignRequest();
        request.setIdCommercant("42");
        request.setIdPdv("8");
        request.setNomCommercial("Epicerie Test");
        request.setTypeAffiliation("TPE");
        request.setRegion("Casablanca");

        var response = controller.affecter("TPE-CREATE", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<Commercant> captor = ArgumentCaptor.forClass(Commercant.class);
        verify(commercantRepository).save(captor.capture());
        Commercant saved = captor.getValue();
        assertEquals("42", saved.getIdCommercant());
        assertEquals("Epicerie Test", saved.getNomCommercial());
        assertEquals("TPE", saved.getTypeAffiliation());
        assertEquals("Casablanca", saved.getRegion());
        assertTrue(saved.isActif());
    }

    @Test
    void assigningWithMerchantDetailsUpdatesExistingCommercantWithoutErasingCreationDate() {
        Tpe available = terminal("TPE-UPDATE", true, null);
        when(repository.findById("TPE-UPDATE")).thenReturn(Optional.of(available));
        when(repository.save(available)).thenReturn(available);

        Commercant existing = new Commercant();
        existing.setIdCommercant("42");
        existing.setNomCommercial("Ancien Nom");
        existing.setActif(false);
        java.time.LocalDateTime originalCreation = java.time.LocalDateTime.of(2025, 1, 1, 0, 0);
        existing.setDateCreation(originalCreation);
        when(commercantRepository.findById("42")).thenReturn(Optional.of(existing));
        when(commercantRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TpeAssignRequest request = new TpeAssignRequest();
        request.setIdCommercant("42");
        request.setIdPdv("8");
        request.setNomCommercial("Nouveau Nom");
        request.setTypeAffiliation("SOFTPOS");
        request.setRegion("Rabat");

        controller.affecter("TPE-UPDATE", request);

        ArgumentCaptor<Commercant> captor = ArgumentCaptor.forClass(Commercant.class);
        verify(commercantRepository).save(captor.capture());
        Commercant saved = captor.getValue();
        assertEquals("Nouveau Nom", saved.getNomCommercial());
        assertTrue(saved.isActif());
        assertEquals(originalCreation, saved.getDateCreation());
    }

    @Test
    void updatesPdvOnlyForAlreadyAssignedTerminal() {
        Tpe assigned = terminal("TPE-REPDV-1", true, "42");
        when(repository.findById("TPE-REPDV-1")).thenReturn(Optional.of(assigned));
        when(repository.save(assigned)).thenReturn(assigned);
        TpeUpdatePdvRequest request = new TpeUpdatePdvRequest();
        request.setIdPdv("99");

        var response = controller.mettreAJourPdv("TPE-REPDV-1", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("99", response.getBody().getIdPdv());
        assertEquals("42", response.getBody().getIdCommercant());
    }

    @Test
    void rejectsPdvUpdateForUnassignedTerminal() {
        Tpe unassigned = terminal("TPE-REPDV-2", true, null);
        when(repository.findById("TPE-REPDV-2")).thenReturn(Optional.of(unassigned));
        TpeUpdatePdvRequest request = new TpeUpdatePdvRequest();
        request.setIdPdv("99");

        var response = controller.mettreAJourPdv("TPE-REPDV-2", request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void pdvUpdateReturns404ForUnknownTerminal() {
        when(repository.findById("TPE-MISSING")).thenReturn(Optional.empty());
        TpeUpdatePdvRequest request = new TpeUpdatePdvRequest();
        request.setIdPdv("99");

        var response = controller.mettreAJourPdv("TPE-MISSING", request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    private Tpe terminal(String id, boolean active, String merchant) {
        Tpe tpe = new Tpe();
        tpe.setIdTpe(id);
        tpe.setNature("TPE");
        tpe.setActif(active);
        tpe.setIdCommercant(merchant);
        return tpe;
    }
}
