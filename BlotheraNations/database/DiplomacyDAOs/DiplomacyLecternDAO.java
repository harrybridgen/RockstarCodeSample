package com.blothera.database.DiplomacyDAOs;

import com.blothera.NationPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DiplomacyLecternDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public DiplomacyLecternDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    /**
     * Saves the location of a diplomacy lectern to the database.
     * If the lectern already exists, it will not be saved again.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void saveLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO diplomacy_lecterns (world, x, y, z) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save diplomacy lectern: " + e.getMessage());
        }
    }

    /**
     * Checks if a lectern at the specified coordinates in the given world is a diplomacy lectern.
     *
     * @param world The name of the world.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     * @return true if the lectern exists and is a diplomacy lectern, false otherwise.
     */
    public boolean isDiplomacyLecternXYZ(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM diplomacy_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check diplomacy lectern: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the given block is a diplomacy lectern.
     *
     * @param lectern The block to check.
     * @return true if the block is a diplomacy lectern, false otherwise.
     */
    public boolean isDiplomacyLectern(Block lectern) {
        if (lectern == null || !(lectern.getState() instanceof Lectern)) {
            return false;
        }
        return isDiplomacyLecternXYZ(lectern.getWorld().getName(), lectern.getX(), lectern.getY(), lectern.getZ());
    }

    /**
     * Deletes a diplomacy lectern from the database.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void deleteLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM diplomacy_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to delete diplomacy lectern: " + e.getMessage());
        }
    }

    /**
     * Determine if a town currently has at least one diplomacy lectern within its claimed chunks.
     *
     * @param townUuid the town to check
     * @param claimDAO DAO providing claim lookups
     * @return true if a lectern exists in any of the town's claims
     */
    public boolean townHasLectern(String townUuid, com.blothera.database.TownDAOs.TownClaimDAO claimDAO) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT world, x, z FROM diplomacy_lecterns"
        )) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int chunkX = rs.getInt("x") >> 4;
                    int chunkZ = rs.getInt("z") >> 4;
                    if (claimDAO.isChunkClaimedByTown(townUuid, world, chunkX, chunkZ)) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check diplomacy lecterns for town: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all diplomacy lecterns from the database.
     *
     * @return A list of LecternRecord objects representing all diplomacy lecterns.
     */
    public java.util.List<LecternRecord> getAllLecterns() {
        java.util.List<LecternRecord> lecterns = new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT world, x, y, z FROM diplomacy_lecterns");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lecterns.add(new LecternRecord(rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch diplomacy lecterns: " + e.getMessage());
        }
        return lecterns;
    }

    /**
     * A record class to represent a lectern's location in the database.
     * This is used to store and retrieve lectern locations efficiently.
     */
    public record LecternRecord(String world, int x, int y, int z) {
    }

    /**
     * Checks if there is a nation lectern in the specified chunk.
     *
     * @param world  The name of the world where the chunk is located.
     * @param chunkX The x-coordinate of the chunk.
     * @param chunkZ The z-coordinate of the chunk.
     * @return true if there is a nation lectern in the chunk, false otherwise.
     */
    public boolean hasLecternInChunk(String world, int chunkX, int chunkZ) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM diplomacy_lecterns WHERE world = ? AND (x >> 4) = ? AND (z >> 4) = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to check for diplomacy lectern in chunk: " + e.getMessage());
            return false;
        }
    }
}
