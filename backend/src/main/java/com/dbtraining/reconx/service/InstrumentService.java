package com.dbtraining.reconx.service;

import com.dbtraining.reconx.exception.InvalidTradeException;
import com.dbtraining.reconx.repository.InstrumentRepository;
import com.dbtraining.reconx.repository.entity.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * TICKET-ADV081 — @Cacheable on findBySymbol (cache name "instruments").
 * TICKET-ADV082 — TTL configured in application.yml (caffeine spec).
 *
 * Symbol lookup is hot — most requests touch the cache, not the DB.
 */
@Service
public class InstrumentService {

    private static final Logger log = LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentRepository repo;

    public InstrumentService(InstrumentRepository repo) { this.repo = repo; }

    @Cacheable(
            value = "instruments",
            key = "#symbol",
            condition = "@reconConfigMBean.isCachingEnabled()"
    )
    public Instrument findBySymbol(String symbol) {
        log.info("DB hit for {}", symbol);
        return repo.findBySymbol(symbol)
                .orElseThrow(() ->
                        new InvalidTradeException("Unknown instrument symbol: " + symbol));
    }
}
