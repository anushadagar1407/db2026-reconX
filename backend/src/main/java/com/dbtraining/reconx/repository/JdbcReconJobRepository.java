package com.dbtraining.reconx.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public class JdbcReconJobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReconJobRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void enqueue(UUID jobId, LocalDate from, LocalDate to, Long counterpartyId) {
        jdbcTemplate.update("""
                INSERT INTO recon_jobs (job_id, from_date, to_date, counterparty_id, status)
                VALUES (?, ?, ?, ?, 'QUEUED')
                """, jobId.toString(), from, to, counterpartyId);
    }
}
