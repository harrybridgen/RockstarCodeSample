package com.blothera.database.WarDAOs;

import com.blothera.NationPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WarAlliesDAO {

    private final NationPlugin plugin;
    private final Connection connection;

    public WarAlliesDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public List<String> getAcceptedAllies(int warId, String side) {
        List<String> allies = new ArrayList<>();
        String sql = """
                    SELECT ally_nation_uuid
                    FROM war_allies
                    WHERE war_id = ? AND side = ? AND accepted = 1
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, side);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    allies.add(rs.getString("ally_nation_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch accepted allies: " + e.getMessage());
        }
        return allies;
    }

    /**
     * Adds a nation to the war allies table as accepted.
     */
    public void addNationToWar(int warId, String nationUuid, String side) {
        String sql = """
                INSERT OR REPLACE INTO war_allies (war_id, ally_nation_uuid, side, accepted)
                VALUES (?, ?, ?, 1);
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, nationUuid);
            stmt.setString(3, side);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add nation to war: " + e.getMessage());
        }
    }

    /**
     * Removes a nation from the war allies table.
     */
    public void removeNationFromWar(int warId, String nationUuid) {
        String sql = "DELETE FROM war_allies WHERE war_id = ? AND ally_nation_uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, nationUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove nation from war: " + e.getMessage());
        }
    }

    /**
     * Checks if a nation is already in the war.
     */
    public boolean isNationInWar(int warId, String nationUuid) {
        String sql = "SELECT 1 FROM war_allies WHERE war_id = ? AND ally_nation_uuid = ? AND accepted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check if nation is in war: " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets the side ('attacker' or 'defender') for a nation in a war.
     */
    public String getSideInWar(int warId, String nationUuid) {
        String sql = "SELECT side FROM war_allies WHERE war_id = ? AND ally_nation_uuid = ? AND accepted = 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, warId);
            stmt.setString(2, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("side");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get war side: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets the war ID the nation is currently involved in, if any.
     */
    public int getOngoingWarIdByNation(String nationUuid) {
        String sql = """
                SELECT war_id FROM war_allies
                WHERE ally_nation_uuid = ? AND accepted = 1
                LIMIT 1;
                """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("war_id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch ongoing war ID from war_allies: " + e.getMessage());
        }
        return -1;
    }
}
