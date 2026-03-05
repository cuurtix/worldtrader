package com.worldtrader.api.market.secure.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.worldtrader.api.market.secure.model.SecureOrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = false)
public record CreateOrderRequest(
        @NotBlank String userId,
        @NotBlank String symbol,
        @NotNull SecureOrderType orderType,
        @NotNull @Min(1) Integer quantity,
        BigDecimal price,
        BigDecimal fee
) {}
