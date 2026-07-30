package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.EquityTrade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcExternalTradeRepository extends JdbcTradeSourceRepository implements ExternalTradeRepository {

    public JdbcExternalTradeRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "EXTERNAL");
    }

    @Override
    public void save(EquityTrade trade) {
        saveTrade(trade);
    }

    @Override
    public List<EquityTrade> findAll() {
        return findAllTrades();
    }
}
