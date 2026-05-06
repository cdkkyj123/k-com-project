package com.example.kcomproject.domain.outbox.repository;

import com.example.kcomproject.domain.outbox.entity.Outbox;
import com.example.kcomproject.domain.outbox.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByStatus(OutboxStatus status);
    List<Outbox> findByStatusIn(Collection<OutboxStatus> statuses);
}
