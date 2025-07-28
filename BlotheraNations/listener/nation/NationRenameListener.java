package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationRenamedEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationRenameListener implements Listener {

    private final NationPlugin plugin;

    public NationRenameListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNationRenamed(NationRenamedEvent event) {
        plugin.getNationLogger().log("Nation " + event.getOldName() + " renamed to " + event.getNewName());
        plugin.getLogger().info("Nation " + event.getOldName() + " renamed to " + event.getNewName());
    }
}
