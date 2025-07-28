package com.blothera.database.TownDAOs;

import com.blothera.NationPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TownJoinRequestDAO {
    private final Connection connection;
    private final NationPlugin plugin;

    public TownJoinRequestDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    /**
     * Creates a join request for a player to a specific town.
     * If a request already exists, it will be replaced.
     *
     * @param playerUuid The UUID of the player making the request.
     * @param townUuid   The UUID of the town to join.
     */
    public void createRequest(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO join_requests (player_uuid, town_uuid) VALUES (?, ?)"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to create join request: " + e.getMessage());
        }
    }

    /**
     * Checks if a player has an existing join request to a specific town.
     *
     * @param playerUuid The UUID of the player.
     * @param townUuid   The UUID of the town.
     * @return true if the request exists, false otherwise.
     */
    public boolean hasRequestToTown(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM join_requests WHERE player_uuid = ? AND town_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to check join request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a list of town UUIDs for which the player has requested to join.
     *
     * @param playerUuid The UUID of the player.
     * @return A list of town UUIDs.
     */
    public List<String> getRequestedTownUuids(String playerUuid) {
        List<String> towns = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT town_uuid FROM join_requests WHERE player_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    towns.add(rs.getString("town_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to fetch requested towns: " + e.getMessage());
        }
        return towns;
    }

    /**
     * Deletes a specific join request for a player to a town.
     *
     * @param playerUuid The UUID of the player.
     * @param townUuid   The UUID of the town.
     */
    public void deleteRequest(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM join_requests WHERE player_uuid = ? AND town_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to delete join request: " + e.getMessage());
        }
    }

    /**
     * Deletes all join requests made by a specific player.
     *
     * @param playerUuid The UUID of the player.
     */
    public void deleteAllRequests(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM join_requests WHERE player_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to delete all join requests for player: " + e.getMessage());
        }
    }

    /**
     * Checks if a player has a join request to a specific town.
     *
     * @param playerUuid The UUID of the player.
     * @param townUuid   The UUID of the town.
     * @return true if the request exists, false otherwise.
     */
    public boolean hasRequestToSpecificTown(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM join_requests WHERE player_uuid = ? AND town_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to verify join request for specific town: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a list of player UUIDs who have requested to join a specific town.
     *
     * @param townUuid The UUID of the town.
     * @return A list of player UUIDs.
     */
    public List<String> getRequestsForTown(String townUuid) {
        List<String> playerUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid FROM join_requests WHERE town_uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    playerUuids.add(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to fetch join requests for town: " + e.getMessage());
        }
        return playerUuids;
    }

}
