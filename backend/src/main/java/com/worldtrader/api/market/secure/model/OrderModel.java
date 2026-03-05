package com.worldtrader.api.market.secure.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_status_created", columnList = "user_id,status,created_at"),
        @Index(name = "idx_orders_symbol_status_price", columnList = "symbol,status,price")
})
public class OrderModel {
    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private SecureOrderType orderType;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 30, scale = 8)
    private BigDecimal price;

    @Column(precision = 30, scale = 8, nullable = false)
    private BigDecimal fee;

    @Column(precision = 30, scale = 8, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public SecureOrderType getOrderType() { return orderType; }
    public void setOrderType(SecureOrderType orderType) { this.orderType = orderType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
}
