package com.blothera.database.NationDAOs;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownRemovedEvent;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class NationDAO {
    private final Connection connection;
    private final NationPlugin plugin;

    public NationDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Fetches the UUID of the capital town for a given nation.
     *
     * @param nationUuid The UUID of the nation.
     * @return The UUID of the capital town, or null if not found.
     */
    public String getCapitalTownUuid(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM towns WHERE nation_uuid = ? AND is_capital = 1"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch capital town UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the name of the nation that a town belongs to.
     *
     * @param townUuid The UUID of the town.
     * @return The name of the nation, or null if not found.
     */
    public String getNationNameByTownUuid(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT n.name FROM nations n JOIN towns t ON n.uuid = t.nation_uuid WHERE t.uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get nation name by town UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves all nation UUIDs from the database.
     *
     * @return A list of nation UUIDs, or an empty list if an error occurs.
     */
    public List<String> getAllNationUuids() {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM nations"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                List<String> nationUuids = new java.util.ArrayList<>();
                while (rs.next()) {
                    nationUuids.add(rs.getString("uuid"));
                }
                return nationUuids;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch all nation UUIDs: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Retrieves the capital town name for a given nation UUID.
     *
     * @param nationUuid The UUID of the nation.
     * @return The name of the capital town, or null if not found.
     */
    public String getCapitalTownName(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM towns WHERE nation_uuid = ? AND is_capital = 1"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch capital town name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the formation date of a nation.
     *
     * @param nationUuid The UUID of the nation.
     * @return The formation date as a String, or null if not found.
     */
    public String getFormationDate(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT founded_at FROM nations WHERE uuid = ?"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("founded_at");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get formation date: " + e.getMessage());
        }
        return null;
    }

    /**
     * Renames a nation in the database.
     *
     * @param nationUuid The UUID of the nation to rename.
     * @param newName    The new name for the nation.
     */
    public void renameNation(String nationUuid, String newName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE nations SET name = ? WHERE uuid = ?"
        )) {
            stmt.setString(1, newName);
            stmt.setString(2, nationUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to rename nation: " + e.getMessage());
        }
    }

    /**
     * Retrieves the canonical name of a nation, ensuring it is stored in the correct case.
     *
     * @param nationName The name of the nation to check.
     * @return The canonical name if found, or null if not found.
     */
    public String getCanonicalName(String nationName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM nations WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, nationName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get canonical nation name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Removes a nation and all its towns from the database.
     *
     * @param nationUuid The UUID of the nation to remove.
     * @return true if the nation was successfully removed, false otherwise.
     */
    public boolean removeNation(String nationUuid) {
        boolean originalAutoCommit = true;

        try {
            // Store current autoCommit setting and disable it for the transaction
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // Remove all towns first (uses TownDAO)
            List<String> townUuids = plugin.getDatabase().getTownDAO().getTownsByNationUuid(nationUuid);
            for (String townUuid : townUuids) {
                String townName = plugin.getDatabase().getTownDAO().getTownName(townUuid);
                boolean wasCapital = plugin.getDatabase().getTownDAO().isCapital(townUuid);
                boolean success = plugin.getDatabase().getTownDAO().removeTown(townUuid);
                if (success) {
                    Bukkit.getPluginManager().callEvent(new TownRemovedEvent(townUuid, townName, nationUuid, wasCapital));
                }
            }

            // Delete nation itself
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM nations WHERE uuid = ?"
            )) {
                stmt.setString(1, nationUuid);
                stmt.executeUpdate();
            }

            connection.commit();
            return true;

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fully remove nation: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Rollback failed: " + ex.getMessage());
            }
            return false;

        } finally {
            try {
                connection.setAutoCommit(originalAutoCommit); // Restore to previous state
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if a nation exists by its name.
     *
     * @param nationName The name of the nation to check.
     * @return true if the nation exists, false otherwise.
     */
    public boolean nationExists(String nationName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nations WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, nationName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nation existence check failed: " + e.getMessage());
            return true;
        }
    }

    /**
     * Retrieves the name of a nation by its UUID.
     *
     * @param nationUuid The UUID of the nation.
     * @return The name of the nation, or null if not found.
     */
    public String getNationName(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM nations WHERE uuid = ?"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nation name fetch failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the UUID of a nation by its name.
     *
     * @param nationName The name of the nation.
     * @return The UUID of the nation, or null if not found.
     */
    public String getNationUUIDByName(String nationName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM nations WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, nationName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nation UUID fetch failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the UUID of the leader of a nation.
     *
     * @param nationUuid The UUID of the nation.
     * @return The UUID of the leader, or null if not found.
     */
    public String getLeaderUuid(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT leader_uuid FROM nations WHERE uuid = ?"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("leader_uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get leader UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if a nation name already exists in the database.
     *
     * @param name The name of the nation to check.
     * @return true if the nation name exists, false otherwise.
     */
    public boolean nationNameExists(String name) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nations WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, name.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Nation name check failed: " + e.getMessage());
            return true; // fail-safe
        }
    }

    public boolean changeLeader(String nationUuid, String newLeaderUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE nations SET leader_uuid = ? WHERE uuid = ?"
        )) {
            stmt.setString(1, newLeaderUuid);
            stmt.setString(2, nationUuid);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to change leader: " + e.getMessage());
            return false;
        }
    }

    public String getNationUuidByTownUuid(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT nation_uuid FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nation_uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get nation UUID by town UUID: " + e.getMessage());
        }
        return null;
    }

    public boolean playerInNation(String uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nation_members WHERE uuid = ?"
        )) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check if player is in nation: " + e.getMessage());
            return false;
        }
    }

    public void createNation(String nationUuid, String name, String leaderUuid, int crownUsed) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO nations (uuid, name, leader_uuid, crown_used) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(1, nationUuid);
            stmt.setString(2, name);
            stmt.setString(3, leaderUuid);
            stmt.setInt(4, crownUsed);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to create nation: " + e.getMessage());
        }
    }

}

