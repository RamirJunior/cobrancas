package com.ramir.cobrancas.repository;

import com.ramir.cobrancas.domain.Cobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {
    Optional<Cobranca> findTopByTxidOrderByIdDesc(String txid);

    Optional<Cobranca> findByTransactionId(String transactionId);
}
