package com.example.kcomproject.domain.outbox.entity;

import com.example.kcomproject.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "outboxes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Outbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = true)
    private String partitionKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    public void sendSuccess() {
        this.status = OutboxStatus.SENT;
    }

    public void sendFailed() {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
    }

    public void moveToDLQ() {
        this.status = OutboxStatus.DLQ;
    }
}
