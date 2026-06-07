package com.ivp.repository;

import com.ivp.domain.model.OutboxEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEntry, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OutboxEntry o WHERE o.published = false ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEntry> findUnpublishedForUpdate(int limit);
}
