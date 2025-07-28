package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownJoinRequestEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownJoinRequestListener implements Listener {

    private final NationPlugin plugin;

    public TownJoinRequestListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoinRequest(TownJoinRequestEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());
        plugin.getNationLogger().log("Player " + event.getPlayerUuid() + " requested to join town " + townName);
        plugin.getLogger().info("Player " + event.getPlayerUuid() + " requested to join town " + townName);
    }
}
