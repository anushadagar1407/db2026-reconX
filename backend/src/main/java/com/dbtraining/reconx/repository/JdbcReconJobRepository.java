package com.dbtraining.reconx.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
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

    public Optional<ReconJobRecord> findNextQueuedJob() {
        var sql = """
                SELECT job_id, from_date, to_date, counterparty_id, status 
                FROM recon_jobs 
                WHERE status = 'QUEUED' 
                LIMIT 1
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ReconJobRecord(
                UUID.fromString(rs.getString("job_id")),
                rs.getObject("from_date", LocalDate.class),
                rs.getObject("to_date", LocalDate.class),
                rs.getObject("counterparty_id", Long.class),
                rs.getString("status")
        )).stream().findFirst();
    }

    public void updateStatus(UUID jobId, String status) {
        jdbcTemplate.update("""
                UPDATE recon_jobs 
                SET status = ? 
                WHERE job_id = ?
                """, status, jobId.toString());
    }

    public record ReconJobRecord(
            UUID jobId,
            LocalDate fromDate,
            LocalDate toDate,
            Long counterpartyId,
            String status
    ) {}


}
