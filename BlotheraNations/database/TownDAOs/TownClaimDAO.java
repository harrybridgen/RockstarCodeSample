package com.blothera.database.TownDAOs;

import com.blothera.NationPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TownClaimDAO {
    private final Connection connection;
    private final NationPlugin plugin;
    private final Map<String, Map<Long, String>> claimCache = new HashMap<>();

    public TownClaimDAO(NationPlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * Generates a unique key for a chunk based on its coordinates.
     * The key is a long value combining chunkX and chunkZ.
     *
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return A long value representing the unique key for the chunk.
     */
    private long getChunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xffffffffL);
    }

    /**
     * Retrieves the town ID for a specific chunk in a given world.
     *
     * @param world  The name of the world.
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return The town ID if the chunk is claimed, null otherwise.
     */
    public String getTownIdAt(String world, int chunkX, int chunkZ) {
        Map<Long, String> worldMap = claimCache.get(world);
        if (worldMap == null) return null;
        return worldMap.get(getChunkKey(chunkX, chunkZ));
    }

    /**
     * Checks if a specific chunk in a given world is claimed by any town.
     *
     * @param world  The name of the world.
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return true if the chunk is claimed, false otherwise.
     */
    public boolean isChunkClaimed(String world, int chunkX, int chunkZ) {
        return getTownIdAt(world, chunkX, chunkZ) != null;
    }

    /**
     * Checks if a specific chunk in a given world is claimed by a specific town.
     *
     * @param townUuid The UUID of the town.
     * @param world    The name of the world.
     * @param chunkX   The X coordinate of the chunk.
     * @param chunkZ   The Z coordinate of the chunk.
     * @return true if the chunk is claimed by the specified town, false otherwise.
     */
    public boolean isChunkClaimedByTown(String townUuid, String world, int chunkX, int chunkZ) {
        String claimOwner = getTownIdAt(world, chunkX, chunkZ);
        return claimOwner != null && claimOwner.equals(townUuid);
    }

    /**
     * Caches all town claims from the database into memory for quick access.
     * This method should be called during plugin initialization to preload claims.
     */
    public void cacheClaims() {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT town_uuid, world, chunk_x, chunk_z FROM town_claims");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String townUuid = rs.getString("town_uuid");
                String world = rs.getString("world");
                int chunkX = rs.getInt("chunk_x");
                int chunkZ = rs.getInt("chunk_z");
                long key = getChunkKey(chunkX, chunkZ);

                claimCache.computeIfAbsent(world, k -> new HashMap<>()).put(key, townUuid);
            }

            plugin.getLogger().info("Loaded " + claimCache.values().stream().mapToInt(Map::size).sum() + " town claims into cache.");

        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load claims into cache: " + e.getMessage());
        }
    }

    public Location getFirstClaimCenterLocation(String townUuid) {
        List<int[]> claims = getClaims(townUuid);
        if (claims.isEmpty()) return null;

        // Use the first claimed chunk
        int[] chunkCoords = claims.getFirst();
        int chunkX = chunkCoords[0];
        int chunkZ = chunkCoords[1];

        // We'll default to the "world" unless you're storing worlds per claim
        World world = Bukkit.getWorlds().getFirst();
        if (world == null) return null;

        // Convert chunk coords to block center (chunks are 16x16)
        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;
        int blockY = world.getHighestBlockYAt(blockX, blockZ); // safe height

        return new Location(world, blockX + 0.5, blockY + 1, blockZ + 0.5);
    }

    /**
     * Retrieves a list of all claims for a specific town.
     *
     * @param townUuid The UUID of the town.
     * @return A list of int arrays, each containing the chunkX and chunkZ of a claimed chunk.
     */
    public List<int[]> getClaims(String townUuid) {
        List<int[]> claims = new ArrayList<>();

        for (Map.Entry<String, Map<Long, String>> worldEntry : claimCache.entrySet()) {
            Map<Long, String> chunkMap = worldEntry.getValue();
            for (Map.Entry<Long, String> chunkEntry : chunkMap.entrySet()) {
                if (townUuid.equals(chunkEntry.getValue())) {
                    long chunkKey = chunkEntry.getKey();
                    int chunkX = (int) (chunkKey >> 32);
                    int chunkZ = (int) chunkKey;
                    claims.add(new int[]{chunkX, chunkZ});
                }
            }
        }

        return claims;
    }

    /**
     * Counts the number of claims made by a specific town.
     *
     * @param townUuid The UUID of the town.
     * @return The number of claims made by the town.
     */
    public int getNumberOfClaims(String townUuid) {
        int count = 0;
        for (Map<Long, String> worldClaims : claimCache.values()) {
            for (String claimOwner : worldClaims.values()) {
                if (townUuid.equals(claimOwner)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Removes all claims associated with a specific town from the database and cache.
     *
     * @param townUuid The UUID of the town whose claims are to be removed.
     */
    public void removeClaimsForTown(String townUuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM town_claims WHERE town_uuid = ?"
        )) {
            stmt.setString(1, townUuid);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                plugin.getLogger().info("Removed " + affectedRows + " claims for town: " + townUuid);
                removeClaimsFromCache(townUuid);
            } else {
                plugin.getLogger().info("No claims found for town: " + townUuid);
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to remove claims for town: " + e.getMessage());
        }
    }

    /**
     * Removes all claims associated with a specific town from the cache.
     *
     * @param townUuid The UUID of the town whose claims are to be removed from the cache.
     */
    void removeClaimsFromCache(String townUuid) {
        for (Map.Entry<String, Map<Long, String>> entry : claimCache.entrySet()) {
            entry.getValue().entrySet().removeIf(e -> townUuid.equals(e.getValue()));
        }
    }


    /**
     * Claims a chunk for a specific town.
     * If the chunk is already claimed, this method does nothing.
     *
     * @param townUuid The UUID of the town claiming the chunk.
     * @param world    The name of the world where the chunk is located.
     * @param chunkX   The X coordinate of the chunk.
     * @param chunkZ   The Z coordinate of the chunk.
     */
    public void claimChunk(String townUuid, String world, int chunkX, int chunkZ) {
        if (isChunkClaimed(world, chunkX, chunkZ)) return;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO town_claims (town_uuid, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?)"
        )) {
            stmt.setString(1, townUuid);
            stmt.setString(2, world);
            stmt.setInt(3, chunkX);
            stmt.setInt(4, chunkZ);
            stmt.executeUpdate();

            // update cache
            claimCache.computeIfAbsent(world, k -> new HashMap<>()).put(getChunkKey(chunkX, chunkZ), townUuid);

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to claim chunk: " + e.getMessage());
        }
    }


    /**
     * Checks if a lectern is located within a town's claimed chunk.
     *
     * @param townUuid The UUID of the town.
     * @param lectern  The lectern block to check.
     * @return true if the lectern is in a claimed chunk, false otherwise.
     */
    public boolean isLecternInTownChunk(String townUuid, Block lectern) {
        if (lectern == null) return false;
        String world = lectern.getWorld().getName();
        int chunkX = lectern.getChunk().getX();
        int chunkZ = lectern.getChunk().getZ();
        return isChunkClaimedByTown(townUuid, world, chunkX, chunkZ);
    }

    /**
     * Retrieves the town UUID at a lectern's location.
     * If the lectern is not in a claimed chunk, returns null.
     *
     * @param lectern The lectern block to check.
     * @return The town UUID if the lectern is in a claimed chunk, null otherwise.
     */
    public String getTownUuidAtLectern(Block lectern) {
        if (lectern == null) return null;
        String world = lectern.getWorld().getName();
        int chunkX = lectern.getChunk().getX();
        int chunkZ = lectern.getChunk().getZ();
        return getTownIdAt(world, chunkX, chunkZ);
    }
}




