package com.blothera.database.NationDAOs;

import com.blothera.NationPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NationLecternDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public NationLecternDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    /**
     * Saves a lectern's location to the database.
     * If the lectern already exists, it will not be saved again.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void saveLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO nation_lecterns (world, x, y, z) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to save nation lectern: " + e.getMessage());
        }
    }

    /**
     * Checks if a lectern at the specified coordinates is a nation lectern.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     * @return true if the lectern is a nation lectern, false otherwise.
     */
    public boolean isNationLecternXYZ(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM nation_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            plugin.getLogger().info("Failed to check if lectern is a nation lectern: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the given block is a nation lectern.
     *
     * @param lectern The block to check.
     * @return true if the block is a nation lectern, false otherwise.
     */
    public boolean isNationLectern(Block lectern) {
        if (lectern == null || !(lectern.getState() instanceof Lectern)) {
            return false;
        }
        return isNationLecternXYZ(lectern.getWorld().getName(), lectern.getX(), lectern.getY(), lectern.getZ());
    }

    /**
     * Deletes a lectern from the database.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void deleteLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM nation_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to delete nation lectern: " + e.getMessage());
        }
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
                "SELECT 1 FROM nation_lecterns WHERE world = ? AND (x >> 4) = ? AND (z >> 4) = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, chunkX);
            stmt.setInt(3, chunkZ);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to check for nation lectern in chunk: " + e.getMessage());
            return false;
        }
    }

    /**
     * Determine if a town currently has at least one nation lectern within its claimed chunks.
     *
     * @param townUuid the town to check
     * @param claimDAO DAO providing claim lookups
     * @return true if a lectern exists in any of the town's claims
     */
    public boolean townHasLectern(String townUuid, com.blothera.database.TownDAOs.TownClaimDAO claimDAO) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT world, x, z FROM nation_lecterns"
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
            plugin.getLogger().info("Failed to check nation lecterns for town: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieves all nation lecterns from the database.
     *
     * @return A list of LecternRecord objects representing all nation lecterns.
     */
    public java.util.List<LecternRecord> getAllLecterns() {
        java.util.List<LecternRecord> lecterns = new java.util.ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT world, x, y, z FROM nation_lecterns");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lecterns.add(new LecternRecord(rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch nation lecterns: " + e.getMessage());
        }
        return lecterns;
    }

    /**
     * A record representing a lectern's location in the database.
     * This is used to store and retrieve lectern data.
     */
    public record LecternRecord(String world, int x, int y, int z) {
    }

}


