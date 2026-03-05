package com.worldtrader.api.market.secure.model;

import jakarta.persistence.*;

@Entity
@Table(name = "positions", indexes = {
        @Index(name = "idx_positions_user_symbol", columnList = "user_id,symbol")
})
public class PositionModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private String userId;
    @Column(nullable = false)
    private String symbol;
}
