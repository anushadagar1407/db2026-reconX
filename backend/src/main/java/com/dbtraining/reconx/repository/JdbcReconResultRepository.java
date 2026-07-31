package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.dto.ReconResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcReconResultRepository implements ReconResultRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReconResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ReconResult result) {
        jdbcTemplate.update("""
                INSERT INTO recon_results (trade_ref, status, discrepancy_type, details)
                VALUES (?, ?, ?, ?)
                """,
                result.tradeRef(),
                result.status().name(),
                result.discrepancyType(),
                result.details());
    }

    @Override
    public List<ReconResult> findAll() {
        return jdbcTemplate.query("""
                SELECT trade_ref, status, discrepancy_type, details
                FROM recon_results
                ORDER BY id
                """,
                (resultSet, rowNumber) -> new ReconResult(
                        resultSet.getString("trade_ref"),
                        ReconResult.Status.valueOf(resultSet.getString("status")),
                        resultSet.getString("discrepancy_type"),
                        resultSet.getString("details")));
    }
}
