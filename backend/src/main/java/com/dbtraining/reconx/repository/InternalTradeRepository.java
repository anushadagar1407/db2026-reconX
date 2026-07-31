package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.EquityTrade;

import java.util.List;

public interface InternalTradeRepository {
    void save(EquityTrade trade);

    List<EquityTrade> findAll();
}
