package com.dbtraining.reconx.repository.entity;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * TICKET-ADV051 — JPA entity Instrument. JSONB metadata column wired via
 * Hypersistence Utils; PostgreSQL uses binary JSONB while H2 uses its
 * portable JSON representation for dev.
 */
@Entity
@Table(name = "instruments")
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 12)
    private String isin;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata = new HashMap<>();

    public Instrument() {}

    public Long getId()         { return id; }
    public String getSymbol()   { return symbol; }
    public String getName()     { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency() { return currency; }
    public String getIsin()     { return isin; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setName(String name) { this.name = name; }
    public void setAssetClass(AssetClass assetClass) { this.assetClass = assetClass; }
    public void setCurrency(String currency) { this.currency = currency; }
    public void setIsin(String isin) { this.isin = isin; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
