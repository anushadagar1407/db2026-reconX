package com.dbtraining.reconx.service;

import com.dbtraining.reconx.model.TradeType;
import com.dbtraining.reconx.repository.ExternalTradeRepository;
import com.dbtraining.reconx.repository.InternalTradeRepository;
import com.dbtraining.reconx.repository.JdbcReconJobRepository;
import com.dbtraining.reconx.repository.ReconBreakRepository;
import com.dbtraining.reconx.repository.entity.ReconBreak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReconJobWorker {

    private static final Logger log = LoggerFactory.getLogger(ReconJobWorker.class);

    private final JdbcReconJobRepository jobRepository;
    private final ReconciliationEngine reconciliationEngine;
    private final ReconBreakRepository breakRepository;
    private final InternalTradeRepository internalTradeRepo;
    private final ExternalTradeRepository externalFeedRepo;

    public ReconJobWorker(JdbcReconJobRepository jobRepository,
                          ReconciliationEngine reconciliationEngine,
                          ReconBreakRepository breakRepository, InternalTradeRepository internalTradeRepo, ExternalTradeRepository externalFeedRepo) {
        this.jobRepository = jobRepository;
        this.reconciliationEngine = reconciliationEngine;
        this.breakRepository = breakRepository;
        this.internalTradeRepo = internalTradeRepo;
        this.externalFeedRepo = externalFeedRepo;
    }

    @Scheduled(fixedDelay = 3000) // Polls every 3 seconds
    public void processNextJob() {
        jobRepository.findNextQueuedJob().ifPresent(job -> {
            log.info("Starting async recon job: jobId={} from={} to={}",
                    job.jobId(), job.fromDate(), job.toDate());

            try {
                // 1. Mark status as RUNNING
//                List<TradeType> internalTrades = internalTradeRepo.findByDateRange(job.fromDate(), job.toDate());
//
//// 2. Fetch external statements/feeds for date range
//                List<TradeType> externalTrades = externalFeedRepo.findByDateRange(job.fromDate(), job.toDate());
//                jobRepository.updateStatus(job.jobId(), "RUNNING");
//
//                // 2. Execute reconciliation logic for the date range
//                List<ReconBreak> generatedBreaks = reconciliationEngine.reconcile(
//                        job.jobId(), job.fromDate(), job.toDate(), job.counterpartyId()
//                );

                // 3. Save breaks tied to this jobId
//                if (!generatedBreaks.isEmpty()) {
//                    breakRepository.saveAll(generatedBreaks);
//                    log.info("Job jobId={} completed with {} break(s)", job.jobId(), generatedBreaks.size());
//                } else {
//                    log.info("Job jobId={} completed with 0 breaks", job.jobId());
//                }

                // 4. Mark status as COMPLETED
                jobRepository.updateStatus(job.jobId(), "COMPLETED");

            } catch (Exception ex) {
                log.error("Failed executing recon job: jobId={}", job.jobId(), ex);
                jobRepository.updateStatus(job.jobId(), "FAILED");
            }
        });
    }
}