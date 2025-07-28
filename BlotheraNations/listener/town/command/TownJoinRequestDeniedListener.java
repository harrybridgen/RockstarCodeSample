package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownJoinRequestDeniedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownJoinRequestDeniedListener implements Listener {

    private final NationPlugin plugin;

    public TownJoinRequestDeniedListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoinDenied(TownJoinRequestDeniedEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());
        plugin.getNationLogger().log("Join request for player " + event.getPlayerUuid() + " denied in town " + townName);
        plugin.getLogger().info("Join request for player " + event.getPlayerUuid() + " denied in town " + townName);
    }
}
