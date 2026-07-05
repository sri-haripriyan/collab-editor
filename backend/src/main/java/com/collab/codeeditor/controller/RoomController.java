package com.collab.codeeditor.controller;

import com.collab.codeeditor.dto.CreateRoomRequest;
import com.collab.codeeditor.dto.PermissionUpdateRequest;
import com.collab.codeeditor.dto.RoomDto;
import com.collab.codeeditor.dto.SnapshotDto;
import com.collab.codeeditor.model.User;
import com.collab.codeeditor.service.AuthService;
import com.collab.codeeditor.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            RoomDto roomDto = roomService.createRoom(request, currentUser);
            return ResponseEntity.ok(roomDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> joinOrGetRoom(@PathVariable("code") String code) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            RoomDto roomDto = roomService.joinOrGetRoom(code, currentUser);
            return ResponseEntity.ok(roomDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<?> getMyRooms() {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        List<RoomDto> rooms = roomService.getUserRooms(currentUser);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/{code}/snapshots")
    public ResponseEntity<?> saveSnapshot(@PathVariable("code") String code, @RequestBody Map<String, String> body) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        String content = body.get("content");
        String language = body.get("language");
        if (content == null || language == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Content and language are required"));
        }
        try {
            SnapshotDto snapshot = roomService.saveSnapshot(code, content, language, currentUser);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/{code}/snapshots")
    public ResponseEntity<?> getSnapshots(@PathVariable("code") String code) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            List<SnapshotDto> snapshots = roomService.getRoomSnapshots(code, currentUser);
            return ResponseEntity.ok(snapshots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/{code}/permissions")
    public ResponseEntity<?> updatePermission(@PathVariable("code") String code, @RequestBody PermissionUpdateRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "Unauthorized"));
        }
        try {
            roomService.updatePermission(code, request.getUsername(), request.getRole(), currentUser);
            return ResponseEntity.ok(Collections.singletonMap("message", "Permission updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}
