package com.dbtraining.reconx.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persisted representation of a failed trade event awaiting operator replay. */
@Entity
@Table(name = "dlq_messages")
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "trade_ref", nullable = false, length = 30)
    private String tradeRef;

    @Column(name = "original_topic", nullable = false)
    private String originalTopic;

    @Column(name = "original_partition", nullable = false)
    private int originalPartition;

    @Column(name = "original_offset", nullable = false)
    private long originalOffset;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    protected DlqMessage() {}

    public DlqMessage(String eventId, String tradeRef, String originalTopic,
                      int originalPartition, long originalOffset, String payload,
                      String reason, Instant firstSeen) {
        this.eventId = eventId;
        this.tradeRef = tradeRef;
        this.originalTopic = originalTopic;
        this.originalPartition = originalPartition;
        this.originalOffset = originalOffset;
        this.payload = payload;
        this.reason = reason;
        this.firstSeen = firstSeen;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public String getTradeRef() { return tradeRef; }
    public String getOriginalTopic() { return originalTopic; }
    public int getOriginalPartition() { return originalPartition; }
    public long getOriginalOffset() { return originalOffset; }
    public String getPayload() { return payload; }
    public String getReason() { return reason; }
    public Instant getFirstSeen() { return firstSeen; }
}
