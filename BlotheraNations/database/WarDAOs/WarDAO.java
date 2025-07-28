package com.blothera.database.WarDAOs;

import com.blothera.NationPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WarDAO {
    private final Connection connection;
    private final NationPlugin plugin;

    public WarDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public record War(int id, int relationId, String attackerNationUuid, String defenderNationUuid) {
    }

    public War getWarById(int warId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, relation_id, attacker_nation_uuid, defender_nation_uuid FROM wars WHERE id = ?"
        )) {
            stmt.setInt(1, warId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new War(
                            rs.getInt("id"),
                            rs.getInt("relation_id"),
                            rs.getString("attacker_nation_uuid"),
                            rs.getString("defender_nation_uuid")
                    );
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch war by ID: " + e.getMessage());
        }
        return null;
    }

    public void deleteWarAndRelation(int warId) {
        try {
            // Fetch relation_id first
            int relationId = -1;
            try (PreparedStatement stmt = connection.prepareStatement(
                    "SELECT relation_id FROM wars WHERE id = ?"
            )) {
                stmt.setInt(1, warId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        relationId = rs.getInt("relation_id");
                    }
                }
            }

            // Delete the relation – cascades to wars, war_battles, and war_allies
            if (relationId != -1) {
                try (PreparedStatement stmt = connection.prepareStatement(
                        "DELETE FROM diplomatic_relations WHERE id = ?"
                )) {
                    stmt.setInt(1, relationId);
                    stmt.executeUpdate();
                }
            }

            plugin.getLogger().info("✅ Deleted war " + warId + " and all associated data.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete war and relation: " + e.getMessage());
        }
    }

    public int getOngoingWarIdByNation(String nationUuid) {
        String sql = """
                SELECT w.id
                FROM wars w
                LEFT JOIN war_allies a ON w.id = a.war_id AND a.accepted = 1
                WHERE w.is_completed = 0 AND (
                    w.attacker_nation_uuid = ? OR
                    w.defender_nation_uuid = ? OR
                    a.ally_nation_uuid = ?
                )
                LIMIT 1;
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nationUuid);
            stmt.setString(2, nationUuid);
            stmt.setString(3, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch ongoing war for nation: " + e.getMessage());
        }

        return -1; // not found
    }

    public boolean isNationInWar(int warId, String nationUuid) {
        String sql = "SELECT 1 FROM war_allies WHERE war_id = ? AND ally_nation_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check nation in war: " + e.getMessage());
        }
        return false;
    }

    public void createWar(int relationId, String attackerUuid, String defenderUuid, String casusBelli, int totalBattles) {
        try (PreparedStatement stmt = connection.prepareStatement("""
                    INSERT INTO wars (relation_id, attacker_nation_uuid, defender_nation_uuid, casus_belli, total_battles)
                    VALUES (?, ?, ?, ?, ?)
                """)) {
            stmt.setInt(1, relationId);
            stmt.setString(2, attackerUuid);
            stmt.setString(3, defenderUuid);
            stmt.setString(4, casusBelli);
            stmt.setInt(5, totalBattles);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create war: " + e.getMessage());
        }
    }

    public int getTotalBattles(int warId) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT total_battles FROM wars WHERE id = ?"
        )) {
            stmt.setInt(1, warId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total_battles");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch total battles for war: " + e.getMessage());
        }
        return 0;
    }

    public int getLatestWarIdByRelation(int relationId) {
        String sql = "SELECT id FROM wars WHERE relation_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, relationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get latest war ID by relation: " + e.getMessage());
        }
        return -1; // return -1 if not found or on error
    }
}
