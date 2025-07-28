package com.blothera.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Minecart;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.blothera.constant.TrainConstants.*;

public class TrainStorageManager {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration config;

    public TrainStorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), TRAINS_YML);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void saveTrains(LinkManager linkManager, ChunkManager chunkManager) {
        config.set(TRAINS_SAVE_CONFIG_ENTRY, null);
        List<Map<String, String>> links = new ArrayList<>();

        for (Minecart cart : Bukkit.getWorlds().stream()
                .flatMap(w -> w.getEntitiesByClass(Minecart.class).stream()).toList()) {
            if (!linkManager.isLinked(cart)) continue;

            UUID childId = cart.getUniqueId();
            UUID parentId = linkManager.getLinkedParentId(cart);
            if (parentId == null) continue;

            Map<String, String> entry = new HashMap<>();
            entry.put(TRAINS_SAVE_CHILD_ENTRY, childId.toString());
            entry.put(TRAINS_SAVE_PARENT_ENTRY, parentId.toString());
            links.add(entry);
        }

        config.set(TRAINS_SAVE_CONFIG_ENTRY, links);
        chunkManager.saveToConfig(config);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save trains.yml: " + e.getMessage());
        }
    }

    public List<Map.Entry<UUID, UUID>> loadTrainLinks() {
        List<Map.Entry<UUID, UUID>> result = new ArrayList<>();
        List<Map<?, ?>> list = config.getMapList(TRAINS_SAVE_CONFIG_ENTRY);
        for (Map<?, ?> map : list) {
            try {
                UUID child = UUID.fromString((String) map.get(TRAINS_SAVE_CHILD_ENTRY));
                UUID parent = UUID.fromString((String) map.get(TRAINS_SAVE_PARENT_ENTRY));
                result.add(Map.entry(child, parent));
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
