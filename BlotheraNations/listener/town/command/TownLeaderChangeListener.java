package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownLeaderChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class TownLeaderChangeListener implements Listener {

    private final NationPlugin plugin;

    public TownLeaderChangeListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTownLeaderChanged(TownLeaderChangedEvent event) {
        OfflinePlayer newLeader = Bukkit.getOfflinePlayer(UUID.fromString(event.getNewLeaderUuid()));
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());
        plugin.getNationLogger().log("Town " + townName + " leadership transferred to " + newLeader.getName());
        plugin.getLogger().info("Town " + townName + " leadership transferred to " + newLeader.getName());
    }
}
