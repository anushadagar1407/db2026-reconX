package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.TradeType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcExternalTradeRepository extends JdbcTradeSourceRepository implements ExternalTradeRepository {

    public JdbcExternalTradeRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "EXTERNAL");
    }

    @Override
    public void save(TradeType trade) {
        saveTrade(trade);
    }

    @Override
    public List<TradeType> findAll() {
        return findAllTrades();
    }
}
