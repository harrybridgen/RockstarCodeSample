package com.blothera.database.NationDAOs;

import com.blothera.NationPlugin;
import com.blothera.database.TownDAOs.TownJoinRequestDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NationMemberDAO {

    private final Connection connection;
    private final NationPlugin plugin;
    private final TownJoinRequestDAO townJoinRequestDAO;

    public NationMemberDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
        this.townJoinRequestDAO = plugin.getDatabase().getJoinRequestDAO();
    }

    /**
     * Adds a player to a nation as a member.
     * If the player is already a member, it updates their membership.
     *
     * @param playerUuid The UUID of the player to add.
     * @param nationUuid The UUID of the nation to add the player to.
     */
    public void addMember(String playerUuid, String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO nation_members (uuid, nation_uuid) VALUES (?, ?)"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, nationUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to add nation member: " + e.getMessage());
        }
    }

    /**
     * Checks if a player is a member of a specific nation.
     *
     * @param playerUuid The UUID of the player to check.
     * @param nationUuid The UUID of the nation to check against.
     * @return true if the player is a member of the nation, false otherwise.
     */
    public boolean isMemberOfNation(String playerUuid, String nationUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nation_members WHERE uuid = ? AND nation_uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Membership check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a player is a member of any nation.
     *
     * @param playerUuid The UUID of the player to check.
     * @return true if the player is a member of any nation, false otherwise.
     */
    public boolean isMember(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nation_members WHERE uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Membership check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the nation UUID for a given player UUID.
     *
     * @param playerUuid The UUID of the player.
     * @return The UUID of the nation the player belongs to, or null if not found.
     */
    public String getNationUuid(String playerUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT nation_uuid FROM nation_members WHERE uuid = ?"
        )) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("nation_uuid");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to get nation UUID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Retrieves a list of all members in a specific nation.
     *
     * @param nationUuid The UUID of the nation to list members for.
     * @return A list of player UUIDs who are members of the specified nation.
     */
    public List<String> getMembers(String nationUuid) {
        List<String> members = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT uuid FROM nation_members WHERE nation_uuid = ?"
        )) {
            stmt.setString(1, nationUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getString("uuid"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to list nation members: " + e.getMessage());
        }
        return members;
    }

    /**
     * Removes a player from the nation and all associated towns.
     * Also removes any pending join requests to towns in the nation.
     *
     * @param playerUuid The UUID of the player to remove.
     * @return true if the player was successfully removed, false otherwise.
     */
    public boolean removeMember(String playerUuid) {
        try {
            // Remove from all towns first
            List<String> townUuids = plugin.getDatabase().getTownMemberDAO().getTownsForPlayer(playerUuid);
            for (String townUuid : townUuids) {
                plugin.getDatabase().getTownMemberDAO().removeMember(playerUuid, townUuid);
            }
            // Remove all pending join requests to towns in the nation
            String nationUuid = plugin.getDatabase().getNationMemberDAO().getNationUuid(playerUuid);
            if (nationUuid != null) {
                List<String> nationTownUuids = plugin.getDatabase().getTownDAO().getTownsByNationUuid(nationUuid);
                for (String townUuid : nationTownUuids) {
                    townJoinRequestDAO.deleteRequest(playerUuid, townUuid);
                }
            }
            // Then remove from nation
            try (PreparedStatement stmt = connection.prepareStatement(
                    "DELETE FROM nation_members WHERE uuid = ?"
            )) {
                stmt.setString(1, playerUuid);
                int affected = stmt.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove nation member: " + e.getMessage());
            return false;
        }
    }

}
