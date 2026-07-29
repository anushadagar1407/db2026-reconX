package com.dbtraining.reconx.service;

import com.dbtraining.reconx.repository.TradeRepository;
import com.dbtraining.reconx.repository.CounterpartyRepository;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.repository.entity.Counterparty;

import java.util.NoSuchElementException;

public class TradeLookupService {

    private final TradeRepository tradeRepo;
    private final CounterpartyRepository cpRepo;

    public TradeLookupService(
           TradeRepository tradeRepo, 
           CounterpartyRepository cpRepo) {
        this.tradeRepo = tradeRepo;
        this.cpRepo = cpRepo;
    }

    public Counterparty counterpartyForTradeRef(String tradeRef) {
        return tradeRepo.findByTradeRef(tradeRef)
        .map(Trade::getCounterparty)
        .orElseThrow(() -> new NoSuchElementException ("No counterparty for tradeRef=" + tradeRef));
    }
}