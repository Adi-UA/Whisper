package dev.adi_ua.whisper.service;

import dev.adi_ua.whisper.model.Group;
import dev.adi_ua.whisper.model.Member;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides CRUD access to groups, members, and rotation history.
 *
 * <p>All methods borrow a connection from the HikariCP pool via
 * {@link DatabaseService#getConnection()} and return it immediately after use
 * via try-with-resources. This is safe for both file-backed SQLite (WAL mode,
 * concurrent reads allowed) and in-memory SQLite (pool size 1, serialized).
 */
@Repository
public class GroupRepository {

    private final DatabaseService db;

    public GroupRepository(DatabaseService db) {
        this.db = db;
    }

    /**
     * Create a new group with the given settings.
     *
     * @param name      display name shown to members
     * @param pinHash   optional PIN (plaintext for M1; bcrypt in M2)
     * @param schedule  rotation frequency: {@code "daily"} or {@code "weekly"}
     * @param timezone  IANA timezone, e.g. {@code "America/Chicago"}
     * @return the persisted {@link Group}
     */
    public Group create(String name, String pinHash, String schedule, String timezone) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Instant now = Instant.now();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO groups (id, name, pin_hash, schedule, timezone, created_at) VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, pinHash);
            ps.setString(4, schedule);
            ps.setString(5, timezone);
            ps.setString(6, now.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create group", e);
        }
        return new Group(id, name, pinHash, schedule, timezone, now);
    }

    /**
     * Look up a group by its ID.
     *
     * @param id the group ID
     * @return an {@link Optional} containing the group, or empty if not found
     */
    public Optional<Group> findById(String id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM groups WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapGroup(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find group", e);
        }
        return Optional.empty();
    }

    /**
     * List all groups. Used by the rotation scheduler to iterate every group.
     *
     * @return all groups in insertion order
     */
    public List<Group> findAll() {
        List<Group> groups = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM groups")) {
            while (rs.next()) {
                groups.add(mapGroup(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list groups", e);
        }
        return groups;
    }

    /**
     * Add a member to an existing group.
     *
     * @param groupId the target group ID
     * @param name    display name for the member
     * @param channel delivery channel in the form {@code "ntfy:<topic>"}
     * @return the persisted {@link Member}
     */
    public Member addMember(String groupId, String name, String channel) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Instant now = Instant.now();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO members (id, group_id, name, channel, joined_at) VALUES (?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, groupId);
            ps.setString(3, name);
            ps.setString(4, channel);
            ps.setString(5, now.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add member", e);
        }
        return new Member(id, groupId, name, channel, now);
    }

    /**
     * List all members of a group.
     *
     * @param groupId the group ID
     * @return members in insertion order
     */
    public List<Member> findMembers(String groupId) {
        List<Member> members = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM members WHERE group_id = ?")) {
            ps.setString(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(new Member(
                        rs.getString("id"),
                        rs.getString("group_id"),
                        rs.getString("name"),
                        rs.getString("channel"),
                        Instant.parse(rs.getString("joined_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find members", e);
        }
        return members;
    }

    /**
     * Return the {@code limit} most recent phrases for a group, newest first.
     * Used as the sliding-window history for the Rust word generator.
     *
     * @param groupId the group ID
     * @param limit   maximum number of phrases to return
     * @return list of phrases, newest first
     */
    public List<String> findHistory(String groupId, int limit) {
        List<String> phrases = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT phrase FROM history WHERE group_id = ? ORDER BY rotated_at DESC LIMIT ?")) {
            ps.setString(1, groupId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    phrases.add(rs.getString("phrase"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find history", e);
        }
        return phrases;
    }

    /**
     * Append a phrase to the rotation history for a group.
     *
     * @param groupId the group ID
     * @param phrase  the phrase that was just generated
     */
    public void addHistory(String groupId, String phrase) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO history (group_id, phrase, rotated_at) VALUES (?,?,?)")) {
            ps.setString(1, groupId);
            ps.setString(2, phrase);
            ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add history", e);
        }
    }

    /**
     * Delete a group and all its members and history.
     *
     * @param groupId the group ID to delete
     */
    public void deleteGroup(String groupId) {
        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM history WHERE group_id = ?")) {
                ps.setString(1, groupId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM members WHERE group_id = ?")) {
                ps.setString(1, groupId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM groups WHERE id = ?")) {
                ps.setString(1, groupId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete group", e);
        }
    }

    /**
     * Remove a single member from a group.
     *
     * @param groupId  the group ID
     * @param memberId the member ID to remove
     */
    public void removeMember(String groupId, String memberId) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "DELETE FROM members WHERE id = ? AND group_id = ?")) {
            ps.setString(1, memberId);
            ps.setString(2, groupId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove member", e);
        }
    }

    private Group mapGroup(ResultSet rs) throws SQLException {
        return new Group(
            rs.getString("id"),
            rs.getString("name"),
            rs.getString("pin_hash"),
            rs.getString("schedule"),
            rs.getString("timezone"),
            Instant.parse(rs.getString("created_at"))
        );
    }
}
