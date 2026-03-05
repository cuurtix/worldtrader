package com.worldtrader.api.market.secure.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balance_audit")
public class BalanceAuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(precision = 30, scale = 8, nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public void setUserId(String userId) { this.userId = userId; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setReason(String reason) { this.reason = reason; }
}
