package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.EquityTrade;

import java.util.List;

public interface ExternalTradeRepository {
    void save(EquityTrade trade);

    List<EquityTrade> findAll();
}
