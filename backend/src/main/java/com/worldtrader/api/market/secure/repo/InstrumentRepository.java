package com.worldtrader.api.market.secure.repo;

import com.worldtrader.api.market.secure.model.InstrumentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<InstrumentModel, String> {
}
