package com.collab.codeeditor.repository;

import com.collab.codeeditor.model.RoomPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomPermissionRepository extends JpaRepository<RoomPermission, Long> {
    Optional<RoomPermission> findByRoomIdAndUserId(Long roomId, Long userId);
    List<RoomPermission> findByRoomId(Long roomId);
}
