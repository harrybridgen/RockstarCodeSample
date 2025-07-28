package com.blothera.database.DiplomacyDAOs;

import com.blothera.NationPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DiplomacyDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public DiplomacyDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Gets the ID of the diplomatic relation between two nations for a given relation type.
     *
     * @param nationA      The UUID of the first nation.
     * @param nationB      The UUID of the second nation.
     * @param relationType The type of the relation (e.g., "alliance", "war").
     * @return The ID of the relation if found, or -1 if not found.
     */
    public int getRelationId(String nationA, String nationB, String relationType) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT id FROM diplomatic_relations " +
                        "WHERE relation_type = ? AND ( (nation_a = ? AND nation_b = ?) OR (nation_a = ? AND nation_b = ?) )"
        )) {
            stmt.setString(1, relationType);
            stmt.setString(2, nationA);
            stmt.setString(3, nationB);
            stmt.setString(4, nationB);
            stmt.setString(5, nationA);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch diplomatic relation ID: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Gets all diplomatic relations between nations.
     * This method retrieves all relations from the database and returns them as a list of Triple objects,
     * where each Triple contains the UUIDs of two nations and the type of relation between them.
     *
     * @return A list of Triple objects representing all diplomatic relations.
     */
    public List<Triple> getAllRelations() {
        List<Triple> relations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT nation_a, nation_b, relation_type FROM diplomatic_relations"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    relations.add(new Triple(
                            rs.getString("nation_a"),
                            rs.getString("nation_b"),
                            rs.getString("relation_type")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch diplomatic relations: " + e.getMessage());
        }
        return relations;
    }


    /**
     * A record representing a diplomatic relation between two nations.
     * This record contains the UUIDs of the two nations and the type of relation between them.
     */
    public record Triple(String nationA, String nationB, String relationType) {
    }


    /**
     * Checks if a diplomatic relation exists between two nations.
     *
     * @param nationUuid       The UUID of the first nation.
     * @param targetNationUuid The UUID of the second nation.
     * @param type             The type of relation to check (e.g., "alliance", "war").
     * @return true if the relation exists, false otherwise.
     */
    public boolean hasRelation(String nationUuid, String targetNationUuid, String type) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM diplomatic_relations " +
                        "WHERE relation_type = ? AND (" +
                        "(nation_a = ? AND nation_b = ?) OR " +
                        "(nation_a = ? AND nation_b = ?))"
        )) {
            stmt.setString(1, type);
            stmt.setString(2, nationUuid);
            stmt.setString(3, targetNationUuid);
            stmt.setString(4, targetNationUuid);
            stmt.setString(5, nationUuid);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check diplomatic relation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks whether the nation is involved in any war relation.
     */
    public boolean hasAnyWarRelation(String nationUuid) {
        String sql = """
                    SELECT 1 FROM diplomatic_relations
                    WHERE relation_type = 'war' AND (nation_a = ? OR nation_b = ?)
                    LIMIT 1;
                """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, nationUuid);
            stmt.setString(2, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check for war relation: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a diplomatic relation between two nations.
     * This method first deletes any pending requests for the specified relation type between the two nations,
     * then inserts a new record into the diplomatic_relations table.
     *
     * @param nationA      The UUID of the first nation.
     * @param nationB      The UUID of the second nation.
     * @param relationType The type of relation to create (e.g., "alliance", "war").
     */
    public void createRelation(String nationA, String nationB, String relationType) {
        plugin.getDatabase().getDiplomacyRequestsDAO().deletePendingRequest(nationA, nationB, relationType);
        plugin.getDatabase().getDiplomacyRequestsDAO().deletePendingRequest(nationB, nationA, relationType);
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO diplomatic_relations (nation_a, nation_b, relation_type) VALUES (?, ?, ?)"
        )) {
            stmt.setString(1, nationA);
            stmt.setString(2, nationB);
            stmt.setString(3, relationType);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create diplomatic relation: " + e.getMessage());
        }
    }

    /**
     * Removes a diplomatic relation between two nations.
     * This method deletes the relation from the diplomatic_relations table based on the specified relation type
     * and the UUIDs of the two nations.
     *
     * @param nationA      The UUID of the first nation.
     * @param nationB      The UUID of the second nation.
     * @param relationType The type of relation to remove (e.g., "alliance", "war").
     * @return true if the relation was successfully removed, false otherwise.
     */
    public boolean removeRelation(String nationA, String nationB, String relationType) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM diplomatic_relations " +
                        "WHERE relation_type = ? AND (" +
                        "(nation_a = ? AND nation_b = ?) OR " +
                        "(nation_a = ? AND nation_b = ?))"
        )) {
            stmt.setString(1, relationType);
            stmt.setString(2, nationA);
            stmt.setString(3, nationB);
            stmt.setString(4, nationB);
            stmt.setString(5, nationA);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove diplomatic relation: " + e.getMessage());
            return false;
        }
    }

}
