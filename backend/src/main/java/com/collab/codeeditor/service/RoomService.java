package com.collab.codeeditor.service;

import com.collab.codeeditor.dto.CreateRoomRequest;
import com.collab.codeeditor.dto.RoomDto;
import com.collab.codeeditor.dto.SnapshotDto;
import com.collab.codeeditor.model.*;
import com.collab.codeeditor.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomPermissionRepository roomPermissionRepository;

    @Autowired
    private CodeSnapshotRepository codeSnapshotRepository;

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request, User owner) {
        String code = generateUniqueRoomCode();
        
        Room room = new Room();
        room.setCode(code);
        room.setName(request.getName());
        room.setOwner(owner);
        room = roomRepository.save(room);

        // Owner gets EDITOR permission entry (implicitly they are OWNER)
        RoomPermission permission = new RoomPermission(room, owner, RoomRole.EDITOR);
        roomPermissionRepository.save(permission);

        // Save an initial snapshot with boilerplate code
        CodeSnapshot initialSnapshot = new CodeSnapshot();
        initialSnapshot.setRoom(room);
        initialSnapshot.setLanguage(request.getLanguage() != null ? request.getLanguage().toLowerCase() : "python");
        initialSnapshot.setContent(getBoilerplateCode(initialSnapshot.getLanguage()));
        initialSnapshot.setSavedBy(owner);
        codeSnapshotRepository.save(initialSnapshot);

        return new RoomDto(room, "OWNER");
    }

    @Transactional
    public RoomDto joinOrGetRoom(String code, User user) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with code: " + code));

        if (!room.isActive()) {
            throw new IllegalArgumentException("This room is no longer active");
        }

        String roleStr = "VIEWER";
        if (room.getOwner().getId().equals(user.getId())) {
            roleStr = "OWNER";
        } else {
            Optional<RoomPermission> permissionOpt = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), user.getId());
            if (permissionOpt.isPresent()) {
                roleStr = permissionOpt.get().getRole().name();
            } else {
                // If it is a new user joining, grant EDITOR permission by default
                RoomPermission newPermission = new RoomPermission(room, user, RoomRole.EDITOR);
                roomPermissionRepository.save(newPermission);
                roleStr = "EDITOR";
            }
        }

        return new RoomDto(room, roleStr);
    }

    public List<RoomDto> getUserRooms(User user) {
        // Rooms where user is owner
        List<Room> ownedRooms = roomRepository.findByOwnerId(user.getId());
        List<RoomDto> roomDtos = ownedRooms.stream()
                .map(room -> new RoomDto(room, "OWNER"))
                .collect(Collectors.toList());

        // Rooms where user participated and is not owner
        List<Room> participatedRooms = roomRepository.findRoomsParticipatedByUserId(user.getId());
        for (Room room : participatedRooms) {
            if (!room.getOwner().getId().equals(user.getId())) {
                Optional<RoomPermission> permissionOpt = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), user.getId());
                String role = permissionOpt.map(p -> p.getRole().name()).orElse("VIEWER");
                roomDtos.add(new RoomDto(room, role));
            }
        }

        // Sort by creation date descending
        roomDtos.sort((r1, r2) -> r2.getCreatedAt().compareTo(r1.getCreatedAt()));
        return roomDtos;
    }

    @Transactional
    public SnapshotDto saveSnapshot(String code, String content, String language, User user) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Verify if user has permission
        if (!room.getOwner().getId().equals(user.getId())) {
            RoomPermission permission = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No access permission for this room"));
            if (permission.getRole() != RoomRole.EDITOR) {
                throw new IllegalArgumentException("Viewer role cannot save snapshots");
            }
        }

        CodeSnapshot snapshot = new CodeSnapshot();
        snapshot.setRoom(room);
        snapshot.setContent(content);
        snapshot.setLanguage(language);
        snapshot.setSavedBy(user);
        
        CodeSnapshot saved = codeSnapshotRepository.save(snapshot);
        return new SnapshotDto(saved);
    }

    public List<SnapshotDto> getRoomSnapshots(String code, User user) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Ensure user has access
        if (!room.getOwner().getId().equals(user.getId())) {
            roomPermissionRepository.findByRoomIdAndUserId(room.getId(), user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No access permission for this room"));
        }

        return codeSnapshotRepository.findByRoomIdOrderBySavedAtDesc(room.getId())
                .stream()
                .map(SnapshotDto::new)
                .collect(Collectors.toList());
    }

    public SnapshotDto getLatestSnapshot(String code) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        return codeSnapshotRepository.findFirstByRoomIdOrderBySavedAtDesc(room.getId())
                .map(SnapshotDto::new)
                .orElse(null);
    }

    @Transactional
    public void updatePermission(String code, String username, String roleStr, User owner) {
        Room room = roomRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Verify if current user is the owner
        if (!room.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Only the room owner can manage permissions");
        }

        User userToUpdate = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (userToUpdate.getId().equals(owner.getId())) {
            throw new IllegalArgumentException("Cannot modify owner's permissions");
        }

        RoomRole role = RoomRole.valueOf(roleStr.toUpperCase());
        RoomPermission permission = roomPermissionRepository.findByRoomIdAndUserId(room.getId(), userToUpdate.getId())
                .orElse(new RoomPermission(room, userToUpdate, role));
        
        permission.setRole(role);
        roomPermissionRepository.save(permission);
    }

    private String generateUniqueRoomCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb;
        do {
            sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            sb.append("-");
            for (int i = 0; i < 3; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
        } while (roomRepository.findByCode(sb.toString()).isPresent());
        
        return sb.toString();
    }

    private String getBoilerplateCode(String language) {
        switch (language.toLowerCase()) {
            case "python":
                return "def main():\n    print(\"Hello, Collaborative Editor!\")\n\nif __name__ == \"__main__\":\n    main()\n";
            case "java":
                return "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Collaborative Editor!\");\n    }\n}\n";
            case "cpp":
            case "c++":
                return "#include <iostream>\n\nint main() {\n    std::cout << \"Hello, Collaborative Editor!\" << std::endl;\n    return 0;\n}\n";
            default:
                return "";
        }
    }
}
