package com.collab.codeeditor.repository;

import com.collab.codeeditor.model.CodeSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSnapshotRepository extends JpaRepository<CodeSnapshot, Long> {
    List<CodeSnapshot> findByRoomIdOrderBySavedAtDesc(Long roomId);
    Optional<CodeSnapshot> findFirstByRoomIdOrderBySavedAtDesc(Long roomId);
}
