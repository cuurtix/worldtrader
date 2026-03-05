package com.worldtrader.api.market.secure.model;

import jakarta.persistence.*;

@Entity
@Table(name = "instruments")
public class InstrumentModel {
    @Id
    private String symbol;
    @Column(nullable = false)
    private boolean tradable = true;
    @Column(nullable = false)
    private int precisionScale;

    public String getSymbol() { return symbol; }
    public boolean isTradable() { return tradable; }
    public int getPrecisionScale() { return precisionScale; }
}
