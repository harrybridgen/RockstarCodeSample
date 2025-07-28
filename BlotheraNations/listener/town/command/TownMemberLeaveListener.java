package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownMemberLeaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownMemberLeaveListener implements Listener {

    private final NationPlugin plugin;

    public TownMemberLeaveListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMemberLeave(TownMemberLeaveEvent event) {
        String townName = plugin.getDatabase().getTownDAO().getTownName(event.getTownUuid());
        plugin.getNationLogger().log("Player " + event.getPlayerUuid() + " left town " + townName);
        plugin.getLogger().info("Player " + event.getPlayerUuid() + " left town " + townName);
    }
}
