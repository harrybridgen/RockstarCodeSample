package com.blothera.listener.town.command;

import com.blothera.NationPlugin;
import com.blothera.event.town.TownRenamedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class TownRenameListener implements Listener {

    private final NationPlugin plugin;

    public TownRenameListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTownRenamed(TownRenamedEvent event) {
        plugin.getNationLogger().log("Town " + event.getOldName() + " renamed to " + event.getNewName());
        plugin.getLogger().info("Town " + event.getOldName() + " renamed to " + event.getNewName());
    }
}
