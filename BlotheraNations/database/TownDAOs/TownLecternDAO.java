package com.blothera.database.TownDAOs;

import com.blothera.NationPlugin;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TownLecternDAO {

    private final Connection connection;
    private final NationPlugin plugin;

    public TownLecternDAO(NationPlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }

    /**
     * Saves the location of a town lectern to the database.
     * If the lectern already exists, it will not be added again.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void saveLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO town_lecterns (world, x, y, z) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to save town lectern: " + e.getMessage());
        }
    }

    /**
     * Checks if a lectern exists at the specified coordinates in the specified world.
     *
     * @param world The name of the world.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     * @return true if a town lectern exists at the specified coordinates, false otherwise.
     */
    public boolean isTownLecternXYZ(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT 1 FROM town_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to check town lectern: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the given block is a town lectern.
     *
     * @param lectern The block to check.
     * @return true if the block is a town lectern, false otherwise.
     */
    public boolean isTownLectern(Block lectern) {
        if (lectern == null || !(lectern.getState() instanceof Lectern)) {
            return false;
        }
        return isTownLecternXYZ(lectern.getWorld().getName(), lectern.getX(), lectern.getY(), lectern.getZ());
    }

    /**
     * Deletes a town lectern from the database.
     *
     * @param world The name of the world where the lectern is located.
     * @param x     The x-coordinate of the lectern.
     * @param y     The y-coordinate of the lectern.
     * @param z     The z-coordinate of the lectern.
     */
    public void deleteLectern(String world, int x, int y, int z) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM town_lecterns WHERE world = ? AND x = ? AND y = ? AND z = ?"
        )) {
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().info("Failed to delete town lectern: " + e.getMessage());
        }
    }

    /**
     * Determine if a town currently has at least one town lectern within its claimed chunks.
     *
     * @param townUuid the town to check
     * @param claimDAO DAO providing claim lookups
     * @return true if a lectern exists in any of the town's claims
     */
    public boolean townHasLectern(String townUuid, TownClaimDAO claimDAO) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT world, x, z FROM town_lecterns"
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
            plugin.getLogger().info("Failed to check lecterns for town: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Retrieves all town lecterns from the database.
     *
     * @return a list of LecternRecord objects representing all town lecterns.
     */
    public List<LecternRecord> getAllLecterns() {
        List<LecternRecord> lecterns = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT world, x, y, z FROM town_lecterns");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lecterns.add(new LecternRecord(rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to fetch town lecterns: " + e.getMessage());
        }
        return lecterns;
    }

    public record LecternRecord(String world, int x, int y, int z) {
    }
}




