package com.realtimecollab.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.realtimecollab.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    @Query("""
            select distinct d
            from Document d
            left join d.collaboratorAccesses access
            where d.owner.id = :userId or access.user.id = :userId
            """)
    List<Document> findAllAccessibleByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct d
            from Document d
            left join d.collaboratorAccesses access
            where d.id = :documentId and (d.owner.id = :userId or access.user.id = :userId)
            """)
    Optional<Document> findAccessibleByIdAndUserId(
            @Param("documentId") Long documentId,
            @Param("userId") Long userId
    );
}
