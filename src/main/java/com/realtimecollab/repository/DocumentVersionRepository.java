package com.realtimecollab.repository;

import com.realtimecollab.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByEditedAtDesc(Long documentId);

    @Transactional
    @Modifying
    @Query("delete from DocumentVersion dv where dv.document.id = :documentId")
    void deleteByDocumentId(Long documentId);
}
