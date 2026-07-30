package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repository;

    public ReconciliationService(ReconciliationEngine engine, ReconResultRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    public void runRecon(List<TradeType> internalTrades, List<TradeType> externalTrades, ReconciliationRule rule) {
        List<ReconResult> results = engine.reconcile(internalTrades, externalTrades, rule);
        for (ReconResult result : results) {
            repository.save(result);
        }
    }
}