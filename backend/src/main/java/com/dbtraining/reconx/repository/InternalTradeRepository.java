package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.TradeType;

import java.util.List;

public interface InternalTradeRepository {
    void save(TradeType trade);

    List<TradeType> findAll();
}
