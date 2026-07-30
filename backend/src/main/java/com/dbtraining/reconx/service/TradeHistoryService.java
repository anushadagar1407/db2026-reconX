package com.dbtraining.reconx.service;

import java.util.List;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dbtraining.reconx.repository.entity.Trade;

import jakarta.persistence.EntityManager;

/**
 * Read-only access to the Envers history of a trade.
 */
@Service
public class TradeHistoryService {

    private final EntityManager entityManager;

    public TradeHistoryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<Number> revisionsFor(Long tradeId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.getRevisions(Trade.class, tradeId);
    }

    @Transactional(readOnly = true)
    public Trade snapshotAt(Long tradeId, Number revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(Trade.class, tradeId, revision);
    }
}
