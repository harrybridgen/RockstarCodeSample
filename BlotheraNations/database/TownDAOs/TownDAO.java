package com.blothera.database.TownDAOs;

import com.blothera.NationPlugin;
import com.blothera.tax.TownTaxTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.blothera.util.NationConstants.*;

public class TownDAO {
    private final Connection connection;
    private final NationPlugin plugin;

    public TownDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Checks if a player is the leader of a specific town.
     *
     * @param playerUuid The UUID of the player.
     * @param townUuid   The UUID of the town.
     * @return true if the player is the leader of the town, false otherwise.
     */
    public boolean isTownLeader(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM towns WHERE leader_uuid = ? AND uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check if player is town leader: " + e.getMessage());
            return false;
        }
    }

    public int getCapitalRadius(String nationUuid) {
        List<String> townUuids = getTownsByNationUuid(nationUuid);
        int townCount = townUuids.size();
        if (townCount < 1) return BASE_BLOCKS_FROM_CAPITAL;

        return BASE_BLOCKS_FROM_CAPITAL + ((townCount - 1) * ADDITIONAL_BLOCK_RADIUS_PER_TOWN_BLOCKS);
    }

    /**
     * Sets a new capital for a nation by updating the is_capital flag in the towns table.
     *
     * @param newCapitalUuid The UUID of the new capital town.
     * @param nationUuid     The UUID of the nation to which the town belongs.
     */
    public void setCapital(String newCapitalUuid, String nationUuid) {
        try {
            connection.setAutoCommit(false);

            // Remove current capital flag
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE towns SET is_capital = 0 WHERE nation_uuid = ?"
            )) {
                stmt.setString(1, nationUuid);
                stmt.executeUpdate();
            }

            // Set new capital
            try (PreparedStatement stmt = connection.prepareStatement(
                    "UPDATE towns SET is_capital = 1 WHERE uuid = ?"
            )) {
                stmt.setString(1, newCapitalUuid);
                stmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set capital: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException ex) {
                plugin.getLogger().severe("Rollback failed: " + ex.getMessage());
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    /**
     * Changes the leader of a town.
     *
     * @param townUuid      The UUID of the town.
     * @param newLeaderUuid The UUID of the new leader.
     */
    public void changeLeader(String townUuid, String newLeaderUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE towns SET leader_uuid = ? WHERE uuid = ?"
        )) {
            stmt.setString(1, newLeaderUuid);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to change town leader: " + e.getMessage());
        }
    }

    /**
     * Fetches the canonical name of a town, ensuring case-insensitivity.
     *
     * @param townName The name of the town to look up.
     * @return The canonical name of the town, or the original name if not found.
     */
    public String getCanonicalTownName(String townName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM towns WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, townName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch canonical town name: " + e.getMessage());
        }
        return townName;
    }

    /**
     * Retrieves the name of a town by its UUID.
     *
     * @param townUuid The UUID of the town.
     * @return The name of the town, or a placeholder if not found.
     */
    public String getTownName(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT name FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch town name: " + e.getMessage());
        }
        return UNKNOWN_TOWN;
    }

    /**
     * Checks if a town with the given name already exists in the database.
     *
     * @param townName The name of the town to check.
     * @return true if the town exists, false otherwise.
     */
    public boolean townExists(String townName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM towns WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, townName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Town name check failed: " + e.getMessage());
            return true;
        }
    }

    /**
     * Renames a town in the database.
     *
     * @param townUuid The UUID of the town to rename.
     * @param newName  The new name for the town.
     */
    public void renameTown(String townUuid, String newName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE towns SET name = ? WHERE uuid = ?"
        )) {
            stmt.setString(1, newName);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to rename town: " + e.getMessage());
        }
    }

    /**
     * Checks if a nation has any towns associated with it.
     *
     * @param nationUuid The UUID of the nation to check.
     * @return true if the nation has at least one town, false otherwise.
     */
    public boolean nationHasAnyTowns(String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM towns WHERE nation_uuid = ? LIMIT 1"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Town check failed: " + e.getMessage());
            return true; // Failsafe
        }
    }

    /**
     * Checks if a player is the leader of any town.
     *
     * @param playerUuid The UUID of the player.
     * @return true if the player is a town leader, false otherwise.
     */
    public boolean isTownLeader(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM towns WHERE leader_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check town leadership: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a town is the capital of its nation.
     *
     * @param townUuid The UUID of the town.
     * @return true if the town is a capital, false otherwise.
     */
    public boolean isCapital(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT is_capital FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("is_capital") == 1;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check if town is capital: " + e.getMessage());
        }
        return false;
    }

    /**
     * Creates a new town in the database.
     *
     * @param uuid       The UUID of the town.
     * @param nationUuid The UUID of the nation to which the town belongs.
     * @param name       The name of the town.
     * @param isCapital  Whether the town is a capital.
     * @param leaderUuid The UUID of the town leader.
     */
    public void createTown(String uuid, String nationUuid, String name, boolean isCapital, String leaderUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO towns (uuid, nation_uuid, name, is_capital, leader_uuid, tax_paid_until, is_dormant) " +
                        "VALUES (?, ?, ?, ?, ?, DATE('now', '+7 day'), 0)"
        )) {
            stmt.setString(1, uuid);
            stmt.setString(2, nationUuid);
            stmt.setString(3, name);
            stmt.setInt(4, isCapital ? 1 : 0);
            stmt.setString(5, leaderUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to register town: " + e.getMessage());
        }
    }

    /**
     * Retrieves all town UUIDs from the database.
     *
     * @return A list of all town UUIDs.
     */
    public List<String> getAllTownUuids() {
        List<String> townUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM towns"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    townUuids.add(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch all town UUIDs: " + e.getMessage());
        }
        return townUuids;
    }

    /**
     * Retrieves the nation UUID associated with a given town UUID.
     *
     * @param townUuid The UUID of the town.
     * @return The UUID of the nation, or null if not found.
     */
    public String getNationUuidFromTownUuid(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT nation_uuid FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("nation_uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch town nation UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the name of the nation associated with a given town UUID.
     *
     * @param townUuid The UUID of the town.
     * @return The name of the nation, or a placeholder if not found.
     */
    public String getNationNameForTown(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT n.name FROM towns t JOIN nations n ON t.nation_uuid = n.uuid WHERE t.uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch nation name for town: " + e.getMessage());
        }
        return UNKNOWN_NATION;
    }

    /**
     * Retrieves a list of town UUIDs associated with a given nation UUID.
     *
     * @param nationUuid The UUID of the nation.
     * @return A list of town UUIDs belonging to the nation.
     */
    public List<String> getTownsByNationUuid(String nationUuid) {
        List<String> townUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM towns WHERE nation_uuid = ?"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    townUuids.add(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch towns for nation: " + e.getMessage());
        }
        return townUuids;
    }

    /**
     * Removes a town from the database.
     *
     * @param townUuid The UUID of the town to remove.
     * @return true if the town was successfully removed, false otherwise.
     */
    public boolean removeTown(String townUuid) {
        try {
            // Remove in-memory claims first (before DB delete to avoid stale cache if failure happens later)
            plugin.getDatabase().getTownClaimDAO().removeClaimsForTown(townUuid);
            plugin.getDatabase().getTownClaimDAO().removeClaimsFromCache(townUuid); // Clear cache for this town, in case of cascade delete of claims

            try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM towns WHERE uuid = ?")) {
                stmt.setString(1, townUuid);
                int affected = stmt.executeUpdate();
                return affected > 0;
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove town: " + e.getMessage());
            return false;
        }
    }


    /**
     * Retrieves the UUID of a town by its name.
     *
     * @param townName The name of the town.
     * @return The UUID of the town, or null if not found.
     */
    public String getTownUuidByName(String townName) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM towns WHERE LOWER(name) = LOWER(?)"
        )) {
            stmt.setString(1, townName.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch town UUID by name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a list of town UUIDs associated with a given player's UUID.
     *
     * @param playerUuid The UUID of the player.
     * @return A list of town UUIDs that the player is a member of.
     */
    public List<String> getTownUuidsByPlayerUuid(String playerUuid) {
        List<String> townUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT t.uuid FROM towns t JOIN town_members m ON m.town_uuid = t.uuid WHERE m.player_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    townUuids.add(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch town UUIDs by player UUID: " + e.getMessage());
        }
        return townUuids;
    }

    /**
     * Retrieves the UUID of the leader of a town.
     *
     * @param townUuid The UUID of the town.
     * @return The UUID of the town leader, or null if not found.
     */
    public String getLeaderUuid(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT leader_uuid FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("leader_uuid");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get town leader UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves the tax payment date for a town.
     *
     * @param townUuid The UUID of the town.
     * @return The date until which taxes have been paid, or null if not found.
     */
    public String getTaxPaidUntil(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT tax_paid_until FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("tax_paid_until");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get tax date: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates the tax payment date for a town.
     *
     * @param townUuid The UUID of the town.
     * @param date     The new tax payment date in YYYY-MM-DD format.
     */
    public void setTaxPaidUntil(String townUuid, String date) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE towns SET tax_paid_until = ? WHERE uuid = ?"
        )) {
            stmt.setString(1, date);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to update tax date: " + e.getMessage());
        }
    }

    /**
     * Checks if a town is dormant (not active).
     *
     * @param townUuid The UUID of the town.
     * @return true if the town is dormant, false otherwise.
     */
    public boolean isDormant(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT is_dormant FROM towns WHERE uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("is_dormant") == 1;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check dormancy: " + e.getMessage());
        }
        return false;
    }

    /**
     * Sets the dormancy status of a town.
     *
     * @param townUuid The UUID of the town.
     * @param dormant  true to set the town as dormant, false otherwise.
     */
    public void setDormant(String townUuid, boolean dormant) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE towns SET is_dormant = ? WHERE uuid = ?"
        )) {
            stmt.setInt(1, dormant ? 1 : 0);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to set dormancy: " + e.getMessage());
        }
    }

    /**
     * Re-evaluates a town's dormancy status based on tax payments and required lecterns.
     * This mirrors the logic used by {@link TownTaxTask} but
     * can be triggered on-demand when lecterns are placed or broken.
     *
     * @param townUuid the town to evaluate
     */
    public void refreshDormancy(String townUuid) {
        String paidUntil = getTaxPaidUntil(townUuid);
        boolean taxesPaid = false;

        if (paidUntil != null) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(paidUntil);
                taxesPaid = !date.isBefore(java.time.LocalDate.now());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse tax date for town " + townUuid + ": " + paidUntil);
            }
        }

        var claimDAO = plugin.getDatabase().getTownClaimDAO();
        var townLecternDAO = plugin.getDatabase().getTownLecternDAO();
        var nationLecternDAO = plugin.getDatabase().getNationLecternDAO();
        var diplomacyLecternDAO = plugin.getDatabase().getDiplomacyLecternDAO();

        boolean hasTownLectern = townLecternDAO.townHasLectern(townUuid, claimDAO);
        boolean isCapital = isCapital(townUuid);
        boolean hasNationLectern = true;
        boolean hasDiplomacyLectern = true;

        if (isCapital) {
            hasNationLectern = nationLecternDAO.townHasLectern(townUuid, claimDAO);
            hasDiplomacyLectern = diplomacyLecternDAO.townHasLectern(townUuid, claimDAO);
        }

        boolean hasRequiredLecterns = isCapital ?
                (hasTownLectern && hasNationLectern && hasDiplomacyLectern) :
                hasTownLectern;

        setDormant(townUuid, !(taxesPaid && hasRequiredLecterns));
    }

}





