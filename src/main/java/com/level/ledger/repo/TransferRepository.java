package com.level.ledger.repo;

import com.level.ledger.domain.Transfer;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Transfer t where t.id = :id")
  Optional<Transfer> findByIdForUpdate(@Param("id") Long id);

  @EntityGraph(attributePaths = {"fromAccount", "toAccount"})
  List<Transfer> findTop50ByOrderByCreatedAtDesc();
}
