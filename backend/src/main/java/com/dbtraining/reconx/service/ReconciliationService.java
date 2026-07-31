package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.ReconciliationRule;
import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ReconResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReconciliationService {

    private final ReconciliationEngine engine;
    private final ReconResultRepository repository;

    public ReconciliationService(ReconciliationEngine engine, ReconResultRepository repository) {
        this.engine = engine;
        this.repository = repository;
    }

    @Transactional
    public void runRecon(List<? extends TradeType> internalTrades,
                         List<? extends TradeType> externalTrades,
                         ReconciliationRule rule) {
        List<ReconResult> results = engine.reconcile(
                internalTrades == null ? null : new ArrayList<>(internalTrades),
                externalTrades == null ? null : new ArrayList<>(externalTrades),
                rule);
        for (ReconResult result : results) {
            repository.save(result);
        }
    }
}
