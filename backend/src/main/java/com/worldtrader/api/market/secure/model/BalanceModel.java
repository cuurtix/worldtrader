package com.worldtrader.api.market.secure.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "balances")
public class BalanceModel {
    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(precision = 30, scale = 8, nullable = false)
    private BigDecimal amount;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
