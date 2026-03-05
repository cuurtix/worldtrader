package com.worldtrader.api.market.secure.repo;

import com.worldtrader.api.market.secure.model.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderModel, String> {
}
