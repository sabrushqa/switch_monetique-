package com.lanacash.switchmonetique.repositories;

import com.lanacash.switchmonetique.entities.TransactionMonetique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<TransactionMonetique, String> {

    List<TransactionMonetique> findByIdCommercantOrderByDateTransactionDesc(String idCommercant);

    List<TransactionMonetique> findByIdTpeAndDateTransactionAfter(String idTpe, LocalDateTime since);

    long countByIdTpeAndDateTransactionAfter(String idTpe, LocalDateTime since);

    @org.springframework.data.jpa.repository.Query(
        "select coalesce(sum(t.montant), 0) from TransactionMonetique t " +
        "where t.idTpe = :idTpe and t.statut = com.lanacash.switchmonetique.entities.enums.StatutTransaction.APPROVED " +
        "and t.dateTransaction >= :since"
    )
    BigDecimal sumMontantApprouveDepuis(String idTpe, LocalDateTime since);
}
