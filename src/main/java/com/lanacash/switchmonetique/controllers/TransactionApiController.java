package com.lanacash.switchmonetique.controllers;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import com.lanacash.switchmonetique.repositories.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de lecture exposee au projet "demo" : demo n'accede jamais directement
 * a la base Oracle du switch, uniquement a ces endpoints REST.
 */
@RestController
@RequestMapping("/api/switch/transactions")
public class TransactionApiController {

    private final TransactionRepository transactionRepository;

    public TransactionApiController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<TransactionMonetique> parCommercant(@RequestParam String commercantId) {
        return transactionRepository.findByIdCommercantOrderByDateTransactionDesc(commercantId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionMonetique> parId(@PathVariable String id) {
        return transactionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
