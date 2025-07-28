package com.blothera.database.DiplomacyDAOs;

import com.blothera.NationPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DiplomacyRequestsDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public DiplomacyRequestsDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Creates a new diplomacy request between two nations.
     *
     * @param fromNationUuid The UUID of the nation sending the request.
     * @param toNationUuid   The UUID of the nation receiving the request.
     * @param type           The type of relation requested (e.g., ALLIANCE, WAR).
     */
    public void createRequest(String fromNationUuid, String toNationUuid, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO diplomacy_requests (from_nation_uuid, to_nation_uuid, relation_type) VALUES (?, ?, ?)"
        )) {
            stmt.setString(1, fromNationUuid);
            stmt.setString(2, toNationUuid);
            stmt.setString(3, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create diplomacy request: " + e.getMessage());
        }
    }

    /**
     * Fetches all diplomacy requests from the database.
     *
     * @return A list of DiplomacyRequest objects representing all requests.
     */
    public List<DiplomacyRequest> getAllRequests() {
        List<DiplomacyRequest> requests = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, from_nation_uuid, to_nation_uuid, relation_type, created_at FROM diplomacy_requests"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(new DiplomacyRequest(
                            rs.getInt("id"),
                            rs.getString("from_nation_uuid"),
                            rs.getString("to_nation_uuid"),
                            rs.getString("relation_type"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch all diplomacy requests: " + e.getMessage());
        }
        return requests;
    }


    /**
     * Deletes all diplomacy requests between two nations.
     *
     * @param nationA The UUID of the first nation.
     * @param nationB The UUID of the second nation.
     */
    public void deleteAllRequests(String nationA, String nationB) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM diplomacy_requests " +
                        "WHERE (from_nation_uuid = ? AND to_nation_uuid = ?) OR " +
                        "(from_nation_uuid = ? AND to_nation_uuid = ?)"
        )) {
            stmt.setString(1, nationA);
            stmt.setString(2, nationB);
            stmt.setString(3, nationB);
            stmt.setString(4, nationA);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete all diplomacy requests: " + e.getMessage());
        }
    }

    /**
     * Deletes a specific pending diplomacy request.
     *
     * @param fromNationUuid The UUID of the nation sending the request.
     * @param toNationUuid   The UUID of the nation receiving the request.
     * @param type           The type of relation requested (e.g., ALLIANCE, WAR).
     */
    public void deletePendingRequest(String fromNationUuid, String toNationUuid, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM diplomacy_requests " +
                        "WHERE from_nation_uuid = ? AND to_nation_uuid = ? AND relation_type = ?"
        )) {
            stmt.setString(1, fromNationUuid);
            stmt.setString(2, toNationUuid);
            stmt.setString(3, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete pending diplomacy request: " + e.getMessage());
        }
    }

    /**
     * Checks if there is a pending diplomacy request between two nations.
     *
     * @param fromNationUuid The UUID of the nation sending the request.
     * @param toNationUuid   The UUID of the nation receiving the request.
     * @param type           The type of relation requested (e.g., ALLIANCE, WAR).
     * @return true if a pending request exists, false otherwise.
     */
    public boolean hasPendingRequestBetween(String fromNationUuid, String toNationUuid, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM diplomacy_requests " +
                        "WHERE status = 'PENDING' AND relation_type = ? " +
                        "AND from_nation_uuid = ? AND to_nation_uuid = ?"
        )) {
            stmt.setString(1, type);
            stmt.setString(2, fromNationUuid);
            stmt.setString(3, toNationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check for existing diplomacy request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves all pending diplomacy requests for a specific nation.
     *
     * @param toNationUuid The UUID of the nation receiving the requests.
     * @return A list of DiplomacyRequest objects representing pending requests.
     */
    public List<DiplomacyRequest> getPendingRequests(String toNationUuid) {
        List<DiplomacyRequest> requests = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, from_nation_uuid, relation_type, created_at FROM diplomacy_requests WHERE to_nation_uuid = ? AND status = 'PENDING'"
        )) {
            stmt.setString(1, toNationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(new DiplomacyRequest(
                            rs.getInt("id"),
                            rs.getString("from_nation_uuid"),
                            toNationUuid,
                            rs.getString("relation_type"),
                            rs.getTimestamp("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch diplomacy requests: " + e.getMessage());
        }
        return requests;
    }

    public record DiplomacyRequest(int id, String fromNationUuid, String toNationUuid, String type,
                                   Timestamp createdAt) {
    }
}
