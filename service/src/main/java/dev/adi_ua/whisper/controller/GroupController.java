package dev.adi_ua.whisper.controller;

import dev.adi_ua.whisper.model.Group;
import dev.adi_ua.whisper.model.Member;
import dev.adi_ua.whisper.service.GroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupRepository repo;

    public GroupController(GroupRepository repo) {
        this.repo = repo;
    }

    public record CreateGroupRequest(String name, String pin, String schedule, String timezone) {}

    @PostMapping
    public Group create(@RequestBody CreateGroupRequest req) {
        String schedule = req.schedule() != null ? req.schedule() : "daily";
        String timezone = req.timezone() != null ? req.timezone() : "America/Chicago";
        // PIN hashing would use bcrypt in production. For M1, store raw (or null).
        return repo.create(req.name(), req.pin(), schedule, timezone);
    }

    @GetMapping
    public List<Group> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public Group get(@PathVariable String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    public record AddMemberRequest(String name, String channel, String pin) {}

    @PostMapping("/{id}/members")
    public Member addMember(@PathVariable String id, @RequestBody AddMemberRequest req) {
        Group group = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        // Validate PIN if set on the group.
        if (group.pinHash() != null && !group.pinHash().isBlank()) {
            if (req.pin() == null || !req.pin().equals(group.pinHash())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid PIN");
            }
        }

        return repo.addMember(id, req.name(), req.channel());
    }

    @GetMapping("/{id}/members")
    public List<Member> listMembers(@PathVariable String id) {
        return repo.findMembers(id);
    }

    @GetMapping("/{id}/history")
    public List<Map<String, String>> history(@PathVariable String id, @RequestParam(defaultValue = "10") int limit) {
        return repo.findHistory(id, limit).stream()
                .map(phrase -> Map.of("phrase", phrase))
                .toList();
    }
}
