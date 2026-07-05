package com.collab.codeeditor.repository;

import com.collab.codeeditor.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByCode(String code);
    
    @Query("SELECT r FROM Room r WHERE r.owner.id = :ownerId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<Room> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT r FROM Room r JOIN RoomPermission rp ON r.id = rp.room.id WHERE rp.user.id = :userId AND r.isActive = true ORDER BY r.createdAt DESC")
    List<Room> findRoomsParticipatedByUserId(@Param("userId") Long userId);
}
