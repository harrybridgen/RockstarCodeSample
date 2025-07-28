package com.blothera.database.TownDAOs;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownMemberExileEvent;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TownMemberDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public TownMemberDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    public void exileBanditFromTowns(String playerUuid) {
        List<String> towns = getTownsForPlayer(playerUuid);

        // Step 1: Fire events BEFORE mutating state
        for (String townUuid : towns) {
            Bukkit.getPluginManager().callEvent(
                    new TownMemberExileEvent(townUuid, playerUuid, "SYSTEM_BANDIT_FLAG")
            );
        }

        // Step 2: Now apply changes AFTER event listeners have handled logic
        for (String townUuid : towns) {
            removeMember(playerUuid, townUuid);
        }

        // Final nation cleanup, now safe
        if (!plugin.getDatabase().getNationMemberDAO().isMember(playerUuid)) return;
        String nationUuid = plugin.getDatabase().getNationMemberDAO().getNationUuid(playerUuid);
        if (nationUuid == null) return;

        List<String> nationTowns = plugin.getDatabase().getTownDAO().getTownsByNationUuid(nationUuid);
        for (String townUuid : nationTowns) {
            if (plugin.getDatabase().getTownMemberDAO().isMemberOfTown(playerUuid, townUuid)) return;
        }

        plugin.getDatabase().getNationMemberDAO().removeMember(playerUuid);
    }


    /**
     * Counts the number of members in a town.
     *
     * @param townUuid The UUID of the town.
     * @return The number of members in the town.
     */
    public int countMembers(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) AS count FROM town_members WHERE town_uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to count members for town: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Fetches the list of members in a town.
     *
     * @param townUuid The UUID of the town.
     * @return A list of player UUIDs who are members of the town.
     */
    public List<String> getMembers(String townUuid) {
        List<String> members = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT player_uuid FROM town_members WHERE town_uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("player_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch members for town: " + e.getMessage());
        }
        return members;
    }

    /**
     * Adds a player as a member of a town.
     *
     * @param playerUuid The UUID of the player to add.
     * @param townUuid   The UUID of the town to add the player to.
     */
    public void addMember(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO town_members (player_uuid, town_uuid) VALUES (?, ?)"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add town member: " + e.getMessage());
        }
    }

    /**
     * Checks if a player is a member of a specific town.
     *
     * @param playerUuid The UUID of the player.
     * @param townUuid   The UUID of the town.
     * @return true if the player is a member of the town, false otherwise.
     */
    public boolean isMemberOfTown(String playerUuid, String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM town_members WHERE player_uuid = ? AND town_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, townUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check town membership: " + e.getMessage());
            return false;
        }
    }

    /**
     * Fetches the list of towns a player is a member of.
     *
     * @param playerUuid The UUID of the player.
     * @return A list of town UUIDs the player is a member of.
     */
    public List<String> getTownsForPlayer(String playerUuid) {
        List<String> townUuids = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT town_uuid FROM town_members WHERE player_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    townUuids.add(rs.getString("town_uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch towns for player: " + e.getMessage());
        }
        return townUuids;
    }

    /**
     * Removes a player from a town and checks if they need to be removed
     * from the nation as well.
     *
     * @param playerUuid The UUID of the player to remove.
     * @param townUuid   The UUID of the town to remove the player from.
     * @return true if the player was removed from the town and possibly the nation, false if they were not a member of the town.
     */
    public boolean removeMember(String playerUuid, String townUuid) {
        try {
            // Remove the player from the town
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM town_members WHERE player_uuid = ? AND town_uuid = ?"
            )) {
                stmt.setString(1, playerUuid);
                stmt.setString(2, townUuid);
                int affected = stmt.executeUpdate();
                if (affected == 0) return false;
            }

            // Get the nation of the town
            String nationUuid = plugin.getDatabase().getTownDAO().getNationUuidFromTownUuid(townUuid);
            if (nationUuid == null) return true; // Not part of a nation, nothing to do

            // Check if the player is still in any town within the same nation
            List<String> playerTowns = getTownsForPlayer(playerUuid);
            for (String otherTown : playerTowns) {
                String otherNation = plugin.getDatabase().getTownDAO().getNationUuidFromTownUuid(otherTown);
                if (nationUuid.equals(otherNation)) {
                    return true; // Still part of another town in the same nation
                }
            }

            // Not in any town in the nation, remove from nation
            plugin.getDatabase().getNationMemberDAO().removeMember(playerUuid);
            return true;

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove town member: " + e.getMessage());
            return false;
        }
    }


    /**
     * Removes all members from a specific town.
     *
     * @param townUuid The UUID of the town to remove all members from.
     */
    public void removeAllTownMembers(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM town_members WHERE town_uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                plugin.getLogger().info("Removed " + affected + " memberships for town: " + townUuid);
            } else {
                plugin.getLogger().info("No memberships found for town: " + townUuid);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove all memberships for town: " + e.getMessage());
        }
    }
}
