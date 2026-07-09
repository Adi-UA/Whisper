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

@Repository
public class GroupRepository {

    private final DatabaseService db;

    public GroupRepository(DatabaseService db) {
        this.db = db;
    }

    public Group create(String name, String pinHash, String schedule, String timezone) {
        String id = UUID.randomUUID().toString().substring(0, 12);
        Instant now = Instant.now();
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO groups (id, name, pin_hash, schedule, timezone, created_at) VALUES (?,?,?,?,?,?)");
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setString(3, pinHash);
            ps.setString(4, schedule);
            ps.setString(5, timezone);
            ps.setString(6, now.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create group", e);
        }
        return new Group(id, name, pinHash, schedule, timezone, now);
    }

    public Optional<Group> findById(String id) {
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM groups WHERE id = ?");
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Group g = mapGroup(rs);
                ps.close();
                return Optional.of(g);
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find group", e);
        }
        return Optional.empty();
    }

    public List<Group> findAll() {
        List<Group> groups = new ArrayList<>();
        try {
            Connection conn = db.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM groups");
            while (rs.next()) {
                groups.add(mapGroup(rs));
            }
            stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list groups", e);
        }
        return groups;
    }

    public Member addMember(String groupId, String name, String channel) {
        String id = UUID.randomUUID().toString().substring(0, 12);
        Instant now = Instant.now();
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO members (id, group_id, name, channel, joined_at) VALUES (?,?,?,?,?)");
            ps.setString(1, id);
            ps.setString(2, groupId);
            ps.setString(3, name);
            ps.setString(4, channel);
            ps.setString(5, now.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add member", e);
        }
        return new Member(id, groupId, name, channel, now);
    }

    public List<Member> findMembers(String groupId) {
        List<Member> members = new ArrayList<>();
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM members WHERE group_id = ?");
            ps.setString(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                members.add(new Member(
                    rs.getString("id"),
                    rs.getString("group_id"),
                    rs.getString("name"),
                    rs.getString("channel"),
                    Instant.parse(rs.getString("joined_at"))
                ));
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find members", e);
        }
        return members;
    }

    public List<String> findHistory(String groupId, int limit) {
        List<String> phrases = new ArrayList<>();
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT phrase FROM history WHERE group_id = ? ORDER BY rotated_at DESC LIMIT ?");
            ps.setString(1, groupId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                phrases.add(rs.getString("phrase"));
            }
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find history", e);
        }
        return phrases;
    }

    public void addHistory(String groupId, String phrase) {
        try {
            Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO history (group_id, phrase, rotated_at) VALUES (?,?,?)");
            ps.setString(1, groupId);
            ps.setString(2, phrase);
            ps.setString(3, Instant.now().toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add history", e);
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
