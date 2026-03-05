package com.worldtrader.api.market.secure.repo;

import com.worldtrader.api.market.secure.model.BalanceAuditModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceAuditRepository extends JpaRepository<BalanceAuditModel, Long> {
}
