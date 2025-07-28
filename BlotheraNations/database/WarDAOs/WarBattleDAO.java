package com.blothera.database.WarDAOs;

import com.blothera.NationPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WarBattleDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public WarBattleDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Schedule a new battle for a war.
     *
     * @param warId         The war id.
     * @param scheduledDate The date of the battle.
     * @param slot          The slot (time) string.
     * @param location      The location string (e.g., coordinates or world name + coords).
     */
    public void scheduleBattle(int warId, LocalDate scheduledDate, String slot, String location) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO war_battles (war_id, scheduled_date, slot, location) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setInt(1, warId);
            stmt.setString(2, scheduledDate.toString());
            stmt.setString(3, slot);
            stmt.setString(4, location);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to schedule battle: " + e.getMessage());
        }
    }

    /**
     * Get all upcoming (not completed) battles for a war.
     *
     * @param warId The war id.
     * @return List of battles.
     */
    public List<WarBattle> getUpcomingBattles(int warId) {
        List<WarBattle> battles = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, scheduled_date, slot, location, is_completed, winner_uuid FROM war_battles " +
                        "WHERE war_id = ? AND is_completed = 0 ORDER BY scheduled_date ASC"
        )) {
            stmt.setInt(1, warId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    battles.add(new WarBattle(
                            rs.getInt("id"),
                            warId,
                            LocalDate.parse(rs.getString("scheduled_date")),
                            rs.getString("slot"),
                            rs.getString("location"),
                            rs.getInt("is_completed") == 1,
                            rs.getString("winner_uuid")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch upcoming battles: " + e.getMessage());
        }
        return battles;
    }

    /**
     * Mark a battle as completed and record winner.
     */
    public void completeBattle(int battleId, String winnerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE war_battles SET is_completed = 1, winner_uuid = ? WHERE id = ?"
        )) {
            stmt.setString(1, winnerUuid);
            stmt.setInt(2, battleId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to complete battle: " + e.getMessage());
        }
    }

    public List<WarBattle> getAllCompletedBattles(int warId) {
        List<WarBattle> battles = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, war_id, scheduled_date, slot, location, is_completed, winner_uuid FROM war_battles WHERE war_id = ? AND is_completed = 1"
        )) {
            stmt.setInt(1, warId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    battles.add(new WarBattle(
                            rs.getInt("id"),
                            rs.getInt("war_id"),
                            LocalDate.parse(rs.getString("scheduled_date")),
                            rs.getString("slot"),
                            rs.getString("location"),
                            rs.getInt("is_completed") == 1,
                            rs.getString("winner_uuid")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch completed battles: " + e.getMessage());
        }
        return battles;
    }

    public List<WarBattle> getAllPendingBattles() {
        List<WarBattle> battles = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, war_id, scheduled_date, slot, location, is_completed, winner_uuid FROM war_battles WHERE is_completed = 0"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    battles.add(new WarBattle(
                            rs.getInt("id"),
                            rs.getInt("war_id"),
                            LocalDate.parse(rs.getString("scheduled_date")),
                            rs.getString("slot"),
                            rs.getString("location"),
                            rs.getInt("is_completed") == 1,
                            rs.getString("winner_uuid")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch pending battles: " + e.getMessage());
        }
        return battles;
    }

    /**
     * Simple record class for battles.
     */
    public record WarBattle(int id, int warId, LocalDate scheduledDate, String slot, String location, boolean completed,
                            String winnerUuid) {
    }
}
