package com.worldtrader.api.market.secure.repo;

import com.worldtrader.api.market.secure.model.BalanceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<BalanceModel, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BalanceModel b where b.userId = :userId")
    Optional<BalanceModel> findByUserIdForUpdate(@Param("userId") String userId);
}
