package com.blothera.manager;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Minecart;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.blothera.constant.TrainConstants.*;

public class ChunkManager {
    private final Set<String> forcedChunkKeys = new HashSet<>();
    private final Map<UUID, String> cartLastChunkKey = new HashMap<>();

    public void updateChunksForCart(Minecart cart) {
        Chunk currentChunk = cart.getLocation().getChunk();
        World world = cart.getWorld();
        UUID cartId = cart.getUniqueId();
        String worldName = world.getName();

        int currentX = currentChunk.getX();
        int currentZ = currentChunk.getZ();
        String currentKey = worldName + "," + currentX + "," + currentZ;

        String lastKey = cartLastChunkKey.get(cartId);
        if (currentKey.equals(lastKey)) return;

        Set<String> newChunks = new HashSet<>();
        for (int dx = -CHUNK_X_LOAD; dx <= CHUNK_X_LOAD; dx++) {
            for (int dz = -CHUNK_Z_LOAD; dz <= CHUNK_Z_LOAD; dz++) {
                int chunkX = currentX + dx;
                int chunkZ = currentZ + dz;
                String key = worldName + "," + chunkX + "," + chunkZ;
                newChunks.add(key);
            }
        }

        Set<String> oldChunks = getOldChunks(lastKey);

        // Unforce only chunks that are no longer needed
        Set<String> toUnforce = new HashSet<>(oldChunks);
        toUnforce.removeAll(newChunks);
        for (String key : toUnforce) {
            String[] parts = key.split(",");
            World w = Bukkit.getWorld(parts[0]);
            if (w != null) {
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                w.getChunkAt(x, z).setForceLoaded(false);
                forcedChunkKeys.remove(key);
            }
        }

        // Force only the new chunks that aren't already loaded
        Set<String> toForce = new HashSet<>(newChunks);
        toForce.removeAll(oldChunks);
        for (String key : toForce) {
            String[] parts = key.split(",");
            World w = Bukkit.getWorld(parts[0]);
            if (w != null) {
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                w.getChunkAt(x, z).setForceLoaded(true);
                forcedChunkKeys.add(key);
            }
        }

        cartLastChunkKey.put(cartId, currentKey);
    }

    public void unforceAllChunksForTrain(Minecart anyCartInTrain) {
        UUID id = anyCartInTrain.getUniqueId();
        String key = cartLastChunkKey.get(id);
        if (key == null) return;

        Set<String> chunksToCheck = getOldChunks(key);

        // Temporarily remove this cart's record
        cartLastChunkKey.remove(id);

        for (String chunkKey : chunksToCheck) {
            boolean stillUsed = false;

            // Check if any *other* cart is still using this chunk
            for (String otherKey : cartLastChunkKey.values()) {
                Set<String> otherChunks = getOldChunks(otherKey);
                if (otherChunks.contains(chunkKey)) {
                    stillUsed = true;
                    break;
                }
            }

            if (!stillUsed) {
                String[] parts = chunkKey.split(",");
                if (parts.length != 3) continue;

                String worldName = parts[0];
                int x = Integer.parseInt(parts[1]);
                int z = Integer.parseInt(parts[2]);
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    world.getChunkAt(x, z).setForceLoaded(false);
                    forcedChunkKeys.remove(chunkKey);
                }
            }
        }
    }

    private static @NotNull Set<String> getOldChunks(String lastKey) {
        Set<String> oldChunks = new HashSet<>();
        if (lastKey != null) {
            String[] parts = lastKey.split(",");
            if (parts.length == 3) {
                String lastWorld = parts[0];
                int lastX = Integer.parseInt(parts[1]);
                int lastZ = Integer.parseInt(parts[2]);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int chunkX = lastX + dx;
                        int chunkZ = lastZ + dz;
                        oldChunks.add(lastWorld + "," + chunkX + "," + chunkZ);
                    }
                }
            }
        }
        return oldChunks;
    }


    public void loadFromConfig(YamlConfiguration config) {
        forcedChunkKeys.clear();
        for (String chunkKey : config.getStringList(TRAINS_SAVE_CHUNKS_ENTRY)) {
            String[] parts = chunkKey.split(",");
            if (parts.length != 3) continue;

            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Chunk chunk = world.getChunkAt(x, z);
                chunk.setForceLoaded(true);
                forcedChunkKeys.add(chunkKey);
            }
        }
    }

    public void saveToConfig(YamlConfiguration config) {
        config.set(TRAINS_SAVE_CHUNKS_ENTRY, new ArrayList<>(forcedChunkKeys));
    }

    public void unforceAll() {
        for (String chunkKey : forcedChunkKeys) {
            String[] parts = chunkKey.split(",");
            if (parts.length != 3) continue;

            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Chunk chunk = world.getChunkAt(x, z);
                chunk.setForceLoaded(false);
            }
        }
        forcedChunkKeys.clear();
        cartLastChunkKey.clear();
    }
}
