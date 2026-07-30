package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.model.EquityTrade;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcInternalTradeRepository extends JdbcTradeSourceRepository implements InternalTradeRepository {

    public JdbcInternalTradeRepository(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate, "INTERNAL");
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
