package com.blothera.listener.nation;

import com.blothera.NationPlugin;
import com.blothera.event.nation.NationMemberExileEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class NationMemberExileListener implements Listener {

    private final NationPlugin plugin;

    public NationMemberExileListener(NationPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMemberExiled(NationMemberExileEvent event) {
        String nationName = plugin.getDatabase().getNationDAO().getNationName(event.getNationUuid());
        plugin.getNationLogger().log("Player " + event.getPlayerUuid() + " exiled from nation " + nationName);
        plugin.getLogger().info("Player " + event.getPlayerUuid() + " exiled from nation " + nationName);
    }
}
